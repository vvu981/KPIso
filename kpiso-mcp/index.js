import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import { CallToolRequestSchema, ListToolsRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import express from "express";
import cors from "cors";
import Groq from "groq-sdk";
import dotenv from "dotenv";

dotenv.config();

const GROQ_API_KEY = process.env.GROQ_API_KEY;
const SCRAPER_URL = process.env.SCRAPER_URL || "http://localhost:8081";
const PORT = process.env.PORT || 3000;

if (!GROQ_API_KEY) {
  console.warn("ADVERTENCIA: GROQ_API_KEY no está configurada en las variables de entorno.");
}

const groq = new Groq({ apiKey: GROQ_API_KEY });

// Inicializar el servidor MCP
const server = new Server(
  {
    name: "kpiso-mcp-server",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Auxiliar para buscar un producto en el catálogo local del scraper
async function searchLocalProduct(term) {
  try {
    const url = `${SCRAPER_URL}/products?q=${encodeURIComponent(term)}`;
    const res = await fetch(url);
    if (!res.ok) return null;
    const products = await res.json();
    return products && products.length > 0 ? products[0] : null;
  } catch (err) {
    console.error(`Error buscando '${term}' en el scraper:`, err);
    return null;
  }
}

// Auxiliar para parsear JSON seguro devuelto por la IA
function extractJsonArray(text) {
  try {
    // Buscar la estructura del array [...] en el texto en caso de que la IA agregue explicaciones
    const match = text.match(/\[\s*".*?"\s*(,\s*".*?"\s*)*\]/s);
    if (match) {
      return JSON.parse(match[0]);
    }
    return JSON.parse(text);
  } catch (e) {
    console.error("Error parseando respuesta de IA a JSON:", text);
    return [];
  }
}

// 1. Declarar las herramientas
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "recommend_recipe_products",
        description: "Analiza una comida y devuelve una lista de ingredientes específicos que existen en el catálogo local products.json.",
        inputSchema: {
          type: "object",
          properties: {
            meal: {
              type: "string",
              description: "Nombre de la comida (ej: 'Tortilla de patatas', 'Pasta carbonara', 'Pizza Margarita')"
            }
          },
          required: ["meal"]
        }
      },
      {
        name: "recommend_diet_products",
        description: "Recomienda productos del catálogo local products.json aptos para una dieta específica.",
        inputSchema: {
          type: "object",
          properties: {
            diet_type: {
              type: "string",
              description: "Tipo de dieta (ej: 'Vegana', 'Keto', 'Sin gluten', 'Baja en calorías')"
            }
          },
          required: ["diet_type"]
        }
      }
    ]
  };
});

// Función interna compartida para ejecutar las herramientas
async function executeTool(name, args) {
  if (name === "recommend_recipe_products") {
    const meal = args?.meal || "";
    if (!meal) {
      throw new Error("El argumento 'meal' es obligatorio.");
    }

    // Consultar a Groq para desglosar la comida en ingredientes básicos
    const prompt = `Actúa como un asistente experto en cocina y compras.
El usuario quiere cocinar: "${meal}".
Identifica los ingredientes básicos de supermercado en español necesarios para cocinar esta receta.
Devuelve ÚNICAMENTE un array en formato JSON con los nombres de los ingredientes (strings) en minúsculas y español.
Ejemplo de salida: ["huevo", "patata", "cebolla", "aceite de oliva"]
No agregues explicaciones, ni introducciones, ni formatees como markdown. Solo el array JSON crudo.`;

    const chatCompletion = await groq.chat.completions.create({
      messages: [{ role: "user", content: prompt }],
      model: "llama-3.3-70b-versatile",
      temperature: 0.1,
    });

    const aiText = chatCompletion.choices[0]?.message?.content || "[]";
    const rawIngredients = extractJsonArray(aiText);

    // Mapear cada ingrediente sugerido con nuestro products.json
    const matchedProducts = [];
    for (const ing of rawIngredients) {
      const match = await searchLocalProduct(ing);
      if (match) {
        matchedProducts.push(match);
      }
    }

    return {
      meal,
      suggestedIngredients: rawIngredients,
      matchedProducts: matchedProducts
    };
  }

  if (name === "recommend_diet_products") {
    const dietType = args?.diet_type || "";
    if (!dietType) {
      throw new Error("El argumento 'diet_type' es obligatorio.");
    }

    // Consultar a Groq para sugerir productos de esa dieta
    const prompt = `Actúa como un nutricionista experto.
El usuario quiere seguir una dieta de tipo: "${dietType}".
Recomienda una lista de 5 a 8 productos de supermercado genéricos comunes (en español) aptos para este tipo de dieta.
Devuelve ÚNICAMENTE un array en formato JSON con los nombres de los productos (strings) en minúsculas y español.
Ejemplo de salida: ["tofu", "lentejas", "aguacate", "espinacas"]
No agregues explicaciones, ni introducciones, ni formatees como markdown. Solo el array JSON crudo.`;

    const chatCompletion = await groq.chat.completions.create({
      messages: [{ role: "user", content: prompt }],
      model: "llama-3.3-70b-versatile",
      temperature: 0.1,
    });

    const aiText = chatCompletion.choices[0]?.message?.content || "[]";
    const rawItems = extractJsonArray(aiText);

    // Mapear cada producto sugerido con nuestro products.json
    const matchedProducts = [];
    for (const item of rawItems) {
      const match = await searchLocalProduct(item);
      if (match) {
        matchedProducts.push(match);
      }
    }

    return {
      dietType,
      suggestedProducts: rawItems,
      matchedProducts: matchedProducts
    };
  }

  throw new Error(`Herramienta no encontrada: ${name}`);
}

// 2. Ejecutar las herramientas vía protocolo MCP
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  try {
    const result = await executeTool(name, args);
    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(result, null, 2)
        }
      ]
    };
  } catch (err) {
    return {
      isError: true,
      content: [
        {
          type: "text",
          text: err.message
        }
      ]
    };
  }
});

// Servidor Express para transporte SSE del protocolo MCP e integración HTTP directa
const app = express();
app.use(cors());
app.use(express.json());

let transport = null;

// Endpoint para iniciar la conexión SSE de MCP
app.get("/sse", async (req, res) => {
  console.log("Nueva conexión SSE de cliente MCP establecida.");
  transport = new SSEServerTransport("/messages", res);
  await server.connect(transport);
});

// Endpoint para el intercambio de mensajes JSON-RPC sobre la conexión SSE establecida
app.post("/messages", async (req, res) => {
  if (!transport) {
    return res.status(400).send("No hay conexión SSE activa.");
  }
  await transport.handlePostMessage(req, res);
});

// Endpoint HTTP directo alternativo (híbrido) para facilidad de integración del backend
app.post("/call-tool", async (req, res) => {
  const { name, arguments: args } = req.body;
  console.log(`Invocación HTTP directa de herramienta: ${name}`);
  try {
    const result = await executeTool(name, args);
    res.json(result);
  } catch (err) {
    console.error(`Error ejecutando herramienta ${name}:`, err);
    res.status(500).json({ error: err.message });
  }
});

// Endpoint de chat integrado para clasificar intenciones e invocar herramientas automáticamente
app.post("/chat", async (req, res) => {
  const { message } = req.body;
  if (!message || !message.trim()) {
    return res.status(400).json({ error: "El mensaje es obligatorio." });
  }

  console.log(`Mensaje de chat recibido: "${message}"`);
  try {
    const systemPrompt = `Eres el asistente de IA inteligente de la lista de compra para el piso compartido KPIso.
Responde SIEMPRE con un objeto JSON válido estructurado como se indica a continuación.
Tu principal objetivo es conversar con el usuario de manera natural y cercana en español, y si pide alimentos o ingredientes para cocinar, proveer una lista de productos para importar.

Analiza el mensaje del usuario y determina:
1. "intent": Debe ser "RECIPE" si está pidiendo activamente recetas o ingredientes para preparar una comida. Debe ser "DIET" si pide activamente recomendaciones de compra o productos para seguir una dieta específica. Debe ser "OTHER" si solo saluda, bromea, pide una definición conceptual, pregunta de teoría o historia sobre una receta/dieta (ej. "¿en qué consiste la dieta X?", "¿quién inventó la carbonara?", o preguntas sobre dietas absurdas/humorísticas).
2. "extractedValue": El nombre del plato o el tipo de dieta si el intent es "RECIPE" o "DIET" (por ejemplo: "lentejas con arroz" o "paleo"), o null en cualquier otro caso.
3. "message": Tu respuesta conversacional, detallada, fluida y amigable en español. No utilices plantillas fijas. Respóndele a su pregunta de manera conversacional, explícales la receta o en qué consiste la dieta si procede. Asegúrate de hablarle con personalidad y claridad.
4. "productsNeeded": Si la intención es RECIPE o DIET, un array con nombres de 4 a 8 productos/ingredientes genéricos comunes en español y en minúsculas (ej: ["huevo", "patata", "cebolla", "aceite de oliva"]) que son necesarios. Si la intención es OTHER, este array debe ser obligatoriamente vacío [].

Formato JSON esperado de respuesta:
{
  "intent": "RECIPE" | "DIET" | "OTHER",
  "extractedValue": string | null,
  "message": "tu respuesta hablada detallada en español",
  "productsNeeded": ["producto1", "producto2", ...]
}
Devuelve ÚNICAMENTE el objeto JSON sin formato markdown ni código alrededor. Solo el JSON.`;

    const chatCompletion = await groq.chat.completions.create({
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: message }
      ],
      model: "llama-3.3-70b-versatile",
      temperature: 0.5,
    });

    const aiResponseText = chatCompletion.choices[0]?.message?.content || "{}";
    let aiResponse = { intent: "OTHER", extractedValue: null, message: "Lo siento, no he podido procesar tu consulta.", productsNeeded: [] };
    
    try {
      const match = aiResponseText.match(/\{.*?\}/s);
      if (match) {
        aiResponse = JSON.parse(match[0]);
      } else {
        aiResponse = JSON.parse(aiResponseText);
      }
    } catch (e) {
      console.error("Fallo al parsear JSON del asistente:", aiResponseText);
      aiResponse.message = aiResponseText;
    }

    console.log("Respuesta procesada de la IA:", aiResponse);

    // Mapear los productos necesarios de la receta/dieta al catálogo local en paralelo
    let matchedProducts = [];
    if (aiResponse.productsNeeded && aiResponse.productsNeeded.length > 0) {
      const searchPromises = aiResponse.productsNeeded.map(term => searchLocalProduct(term));
      const results = await Promise.all(searchPromises);
      matchedProducts = results.filter(product => product !== null);
    }

    return res.json({
      intent: aiResponse.intent || "OTHER",
      message: aiResponse.message,
      products: matchedProducts
    });

  } catch (err) {
    console.error("Error en endpoint /chat:", err);
    res.status(500).json({ error: "Error al procesar la petición con la IA: " + err.message });
  }
});

app.listen(PORT, () => {
  console.log(`Servidor MCP ejecutándose en http://localhost:${PORT}`);
  console.log(`Conexión SSE disponible en http://localhost:${PORT}/sse`);
});
