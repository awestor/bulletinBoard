package ru.daniil.bulletinBoard.entity.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentMethodResponse {
    private Long id;
    private String type;

    public PaymentMethodResponse(){
    }

    public PaymentMethodResponse(Long id, String type){
        this.id = id;
        this.type = type;
    }
}
