package com.tugba.radyoloji_raporlama_asistani.entity;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor 
@Getter 
@Setter 
@Table(name = "app_user")
@Entity 
public class AppUser {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Foreign key Identity tablosunda (app_user_id); burasi sadece ters yon.
    @OneToOne(mappedBy = "appUser", fetch = FetchType.LAZY)
    private Identity identity;

    /** Ekranda gosterilecek ad: kimlik varsa "Unvan Ad Soyad", yoksa e-posta. */
    public String getDisplayName() {
        if (identity == null) {
            return email;
        }
        return identity.getTitle() == null
                ? identity.getFullName()
                : identity.getTitle().getDisplayName() + " " + identity.getFullName();
    }

}
