package br.com.eliasssantana;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UrlData {

    private String originalUrl;
    private Long expirationTime;
}
