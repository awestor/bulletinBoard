package ru.daniil.bulletinBoard.entity.response;

public class PaymentMethodResponse {
    private Long id;
    private String type;

    public PaymentMethodResponse(){
    }

    public PaymentMethodResponse(Long id, String type){
        this.id = id;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
