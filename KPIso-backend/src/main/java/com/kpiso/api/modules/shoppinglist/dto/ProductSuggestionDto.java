package com.kpiso.api.modules.shoppinglist.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSuggestionDto {

    private String name;
    private String imageUrl;
    private String categoryTags;
    private Double estimatedPrice;
}
