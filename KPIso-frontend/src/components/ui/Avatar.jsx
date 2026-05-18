/**
 * Avatar Component
 * 
 * Displays a user avatar with customizable sizes, colors, and optional initials fallback.
 * Follows SOLID principles with single responsibility (avatar display), extensible through props,
 * and substitutable in different contexts.
 * 
 * @component
 * @example
 * // Basic usage with image
 * <Avatar src="https://example.com/avatar.jpg" alt="John Doe" />
 * 
 * @example
 * // With initials fallback
 * <Avatar name="Jane Smith" size="lg" />
 * 
 * @example
 * // With custom color
 * <Avatar name="Bob Johnson" color="#3b82f6" size="md" />
 */

export function Avatar({
    src = null,
    alt = "User avatar",
    name = null,
    size = "md",
    color = "#6366f1",
    className = ""
}) {
    /**
     * Size mapping to CSS classes
     * @type {Object<string, string>}
     */
    const sizeMap = {
        xs: "w-6 h-6 text-[10px]",
        sm: "w-8 h-8 text-xs",
        md: "w-10 h-10 text-sm",
        lg: "w-12 h-12 text-base",
        xl: "w-16 h-16 text-lg",
        "2xl": "w-20 h-20 text-xl"
    };

    /**
     * Get initials from name
     * @param {string} fullName - Full name of user
     * @returns {string} Initials (max 2 characters)
     */
    const getInitials = (fullName) => {
        if (!fullName) return "?";
        return fullName
            .split(" ")
            .map((word) => word.charAt(0).toUpperCase())
            .slice(0, 2)
            .join("");
    };

    const initials = name ? getInitials(name) : "?";
    const sizeClass = sizeMap[size] || sizeMap.md;

    return (
        <div
            className={`
                inline-flex items-center justify-center
                rounded-full font-bold flex-shrink-0
                ${src ? "bg-gray-200" : "bg-opacity-10 text-white"}
                border border-black/5 shadow-xs
                ${sizeClass}
                ${className}
            `}
            style={{ backgroundColor: !src ? color : undefined }}
            role="img"
            aria-label={alt || name || "User avatar"}
            title={name || alt}
        >
            {src ? (
                <img
                    src={src}
                    alt={alt || name}
                    className="w-full h-full object-cover rounded-full"
                />
            ) : (
                <span className="select-none">{initials}</span>
            )}
        </div>
    );
}
