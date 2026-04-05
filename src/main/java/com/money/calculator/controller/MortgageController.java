package com.money.calculator.controller;

import com.money.calculator.dto.MortgageRequest;
import com.money.calculator.dto.MortgageResult;
import com.money.calculator.service.MortgageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class MortgageController {

    private final MortgageService mortgageService;

    @GetMapping
    public String showForm(Model model) {
        if (!model.containsAttribute("mortgageRequest")) {
            model.addAttribute("mortgageRequest", new MortgageRequest());
        }

        return "mortgage";
    }

    @PostMapping
    public String calculate(@Valid @ModelAttribute MortgageRequest mortgageRequest,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.mortgageRequest", bindingResult);
            redirectAttributes.addFlashAttribute("mortgageRequest", mortgageRequest);

            return "redirect:/";
        }

        try {
            MortgageResult result = mortgageService.calculate(mortgageRequest);

            redirectAttributes.addFlashAttribute("mortgageRequest", mortgageRequest);
            redirectAttributes.addFlashAttribute("result", result);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mortgageRequest", mortgageRequest);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/";
    }
}