package org.spring.mvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Autorun
 * Created by Autorun on 2018/1/9.
 */

@RestController
public class HelloController {

    @GetMapping("/") @ResponseBody
    public String index() {
        return "index";
    }
}
