package com.lezenford.mfr.server.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ManualController {

    @GetMapping("/readme")
    fun manual() = "redirect:readme/index.html"
}