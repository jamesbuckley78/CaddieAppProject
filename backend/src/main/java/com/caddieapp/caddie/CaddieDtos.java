package com.caddieapp.caddie;

import com.caddieapp.common.Enums.Grade;
import com.caddieapp.common.Enums.Placement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CaddieDtos {

    private CaddieDtos() {
    }

    public record CreateCaddieRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 ()\\-.]{7,25}$", message = "is not a valid phone number") String phone,
        @NotNull Grade grade,
        boolean priority,
        /** null = TOP_OF_GROUP (a new caddie). BOTTOM_OF_GROUP when copying the paper list in order. */
        Placement placement) {
    }

    public record CaddieResponse(
        long id,
        long userId,
        String name,
        String phone,
        Grade grade,
        boolean priority,
        int listPosition,
        /** false until the caddie has logged in and replaced their setup code */
        boolean passwordSet) {

        static CaddieResponse from(Caddie caddie) {
            User user = caddie.getUser();
            return new CaddieResponse(caddie.getId(), user.getId(), user.getName(), user.getPhone(),
                caddie.getGrade(), caddie.isPriority(), caddie.getListPosition(), !user.isMustChangePassword());
        }
    }

    /** setupCode is only ever shown once - it's stored hashed, like a password. */
    public record CaddieWithSetupCode(CaddieResponse caddie, String setupCode) {
    }
}
