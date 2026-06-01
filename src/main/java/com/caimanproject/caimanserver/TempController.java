package com.caimanproject.caimanserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TempController {

    @GetMapping("/tst")
    public Map<String,String> testEndpoint() {
        return Map.of(
            "msg", "is working!!!"
                     );
    }
}
