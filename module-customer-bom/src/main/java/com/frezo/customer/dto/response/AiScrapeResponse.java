package com.frezo.customer.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AiScrapeResponse {
    private String status;
    private List<AiLeadData> data;

    @Data
    public static class AiLeadData {
        private String name;
        private String phone;
        private String address;
        private String url;
        private String ward;
        private String city;
    }
}
