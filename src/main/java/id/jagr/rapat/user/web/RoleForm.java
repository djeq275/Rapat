package id.jagr.rapat.user.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleForm {

    @NotBlank(message = "Nama role wajib diisi")
    private String name;
    private boolean requiresDivision;
    private boolean autoInviteToAllMeetings;
    private boolean canViewAllDivisions;
    private boolean canOrganizeMeetings;
    private List<Long> capabilityIds = new ArrayList<>();
}
