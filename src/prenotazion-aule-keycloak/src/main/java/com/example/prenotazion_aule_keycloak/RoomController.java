package com.example.prenotazion_aule_keycloak;

import com.example.prenotazion_aule_keycloak.security.KeycloakPdpService;
import com.example.prenotazion_aule_keycloak.service.BookingService;
import com.example.prenotazion_aule_keycloak.service.KeycloakService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class RoomController {

    private final BookingService bookingService;
    private final KeycloakService keycloakService;
    private final KeycloakPdpService pdpService;

    public RoomController(BookingService bookingService,
                          KeycloakService keycloakService,
                          KeycloakPdpService pdpService) {
        this.bookingService = bookingService;
        this.keycloakService = keycloakService;
        this.pdpService = pdpService;
    }

    @GetMapping("/")
    public String index() { return "redirect:/user"; }

    // =================================================================================
    //  AREA UTENTE (Gestita con Fine-Grained Auth)
    // =================================================================================

    // 1. VISUALIZZAZIONE DASHBOARD (Scope: view)
    // Usa ancora @PreAuthorize perché è solo visualizzazione pagina
    @PreAuthorize("@pdp.check(authentication, 'User Resource', 'view')")
    @GetMapping("/user")
    public String userArea(@AuthenticationPrincipal OidcUser principal, Model model) {
        String username = principal.getPreferredUsername();
        model.addAttribute("username", username);
        model.addAttribute("rooms", bookingService.getAllRooms());
        model.addAttribute("myBookings", bookingService.getBookingsByUser(username));
        return "user";
    }

    // 2. CREAZIONE PRENOTAZIONE (Scope: create)
    @PostMapping("/user/book")
    public String bookRoom(@AuthenticationPrincipal OidcUser principal,
                           @RequestParam Long roomId,
                           @RequestParam String date,
                           Authentication authentication) { // <--- Serve Authentication per il PDP

        // --- CHECK SICUREZZA KEYCLOAK (Can I create?) ---
        if (!pdpService.check(authentication, "User Resource", "create")) {
            return "redirect:/user?error=Accesso Negato: Non hai i permessi per creare prenotazioni.";
        }

        try {
            bookingService.createBooking(principal.getPreferredUsername(), roomId, LocalDate.parse(date));
            return "redirect:/user?message=Prenotazione confermata!";
        } catch (Exception e) {
            return "redirect:/user?error=" + e.getMessage();
        }
    }

    // 3. CANCELLAZIONE PRENOTAZIONE UTENTE (Scope: delete + Controllo Proprietà)
    @PostMapping("/user/delete/{id}")
    public String deleteMyBooking(@PathVariable Long id,
                                  @AuthenticationPrincipal OidcUser principal,
                                  Authentication authentication) { // <--- Serve Authentication per il PDP

        // --- CHECK 1: KEYCLOAK (Ho il ruolo per cancellare in generale?) ---
        if (!pdpService.check(authentication, "User Resource", "delete")) {
            return "redirect:/user?error=Accesso Negato: Il tuo ruolo non permette cancellazioni.";
        }

        try {
            // --- CHECK 2: JAVA LOGIC (La prenotazione è davvero mia?) ---
            bookingService.deleteBookingIfOwner(id, principal.getPreferredUsername());
            return "redirect:/user?message=Cancellata con successo.";
        } catch (Exception e) {
            return "redirect:/user?error=" + e.getMessage();
        }
    }

    // =================================================================================
    //  GESTIONE PROFILO
    // =================================================================================

    @PreAuthorize("@pdp.check(authentication, 'User Resource', 'view')")
    @GetMapping("/user/profile")
    public String userProfile(@AuthenticationPrincipal OidcUser principal, Model model) {
        model.addAttribute("username", principal.getPreferredUsername());
        model.addAttribute("email", principal.getEmail());
        model.addAttribute("fullName", principal.getFullName());
        model.addAttribute("userId", principal.getSubject());
        return "profile";
    }

    @PreAuthorize("@pdp.check(authentication, 'User Resource', 'view')")
    @GetMapping("/user/profile/password")
    public String showChangePasswordPage() { return "password_form"; }

    @PreAuthorize("@pdp.check(authentication, 'User Resource', 'view')")
    @PostMapping("/user/profile/password")
    public String processChangePassword(@AuthenticationPrincipal OidcUser principal,
                                        @RequestParam String password,
                                        @RequestParam String confirmPassword, Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Le password non coincidono!");
            return "password_form";
        }
        try {
            keycloakService.changePassword(principal.getSubject(), password);
            return "redirect:/user/profile?message=Password aggiornata!";
        } catch (Exception e) {
            model.addAttribute("error", "Errore: " + e.getMessage());
            return "password_form";
        }
    }

    @PreAuthorize("@pdp.check(authentication, 'User Resource', 'view')")
    @GetMapping("/user/profile/email")
    public String showChangeEmailPage(@AuthenticationPrincipal OidcUser principal, Model model) {
        model.addAttribute("currentEmail", principal.getEmail());
        return "email_form";
    }

    @PreAuthorize("@pdp.check(authentication, 'User Resource', 'view')")
    @PostMapping("/user/profile/email")
    public String processChangeEmail(@AuthenticationPrincipal OidcUser principal,
                                     @RequestParam String newEmail, Model model) {
        try {
            keycloakService.updateEmail(principal.getSubject(), newEmail);
            return "redirect:/user/profile?message=Email aggiornata! Al prossimo login vedrai la modifica.";
        } catch (Exception e) {
            model.addAttribute("error", "Errore cambio email: " + e.getMessage());
            model.addAttribute("currentEmail", principal.getEmail());
            return "email_form";
        }
    }

    @PreAuthorize("@pdp.check(authentication, 'User Resource', 'view')")
    @PostMapping("/user/profile/delete")
    public String deleteFullAccount(@AuthenticationPrincipal OidcUser principal, HttpServletRequest request) {
        try {
            bookingService.deleteUserData(principal.getPreferredUsername());
            keycloakService.deleteUserFromKeycloak(principal.getSubject());
            request.logout();
            return "redirect:/";
        } catch (Exception e) {
            return "redirect:/user/profile?error=Impossibile eliminare account: " + e.getMessage();
        }
    }

    // =================================================================================
    //  AREA AMMINISTRATORE (Admin Resource + Scope Delete)
    // =================================================================================

    @PreAuthorize("@pdp.check(authentication, 'Admin Resource', 'view')")
    @GetMapping("/admin")
    public String adminArea(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "admin";
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteBookingAdmin(@PathVariable Long id, Authentication authentication) {

        // Questo usa la risorsa "res-prenotazione" e la policy "Policy Only Admin"
        if (!pdpService.check(authentication, "res-prenotazione", "delete")) {
            return "redirect:/admin?error=Accesso Negato: Non hai il permesso di eliminare prenotazioni.";
        }

        bookingService.deleteBooking(id);
        return "redirect:/admin?message=Prenotazione eliminata con successo.";
    }
}