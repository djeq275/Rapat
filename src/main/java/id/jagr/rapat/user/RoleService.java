package id.jagr.rapat.user;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.jagr.rapat.user.web.RoleForm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final AppRoleRepository appRoleRepository;
    private final CapabilityRepository capabilityRepository;
    private final UserRepository userRepository;

    public List<AppRole> findAll() {
        return appRoleRepository.findAllWithCapabilities();
    }

    public AppRole findById(Long id) {
        return appRoleRepository.findByIdWithCapabilities(id)
                .orElseThrow(() -> new IllegalArgumentException("Role tidak ditemukan"));
    }

    public List<Capability> findAllCapabilities() {
        return capabilityRepository.findAll();
    }

    public long countUsersWithRole(Long roleId) {
        return userRepository.countByRole(findById(roleId));
    }

    @Transactional
    public AppRole create(RoleForm form) {
        assertNameAvailable(form.getName(), null);
        AppRole role = new AppRole();
        role.setName(form.getName());
        role.setBuiltIn(false);
        applyForm(role, form);
        return appRoleRepository.save(role);
    }

    @Transactional
    public AppRole update(Long id, RoleForm form) {
        AppRole role = findById(id);
        assertNotBuiltIn(role, "diubah");
        assertNameAvailable(form.getName(), id);
        role.setName(form.getName());
        applyForm(role, form);
        return appRoleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        AppRole role = findById(id);
        assertNotBuiltIn(role, "dihapus");
        if (userRepository.existsByRole(role)) {
            throw new IllegalArgumentException("Role masih dipakai oleh pengguna, tidak bisa dihapus");
        }
        appRoleRepository.delete(role);
    }

    private void applyForm(AppRole role, RoleForm form) {
        role.setRequiresDivision(form.isRequiresDivision());
        role.setAutoInviteToAllMeetings(form.isAutoInviteToAllMeetings());
        role.setCanViewAllDivisions(form.isCanViewAllDivisions());
        role.setCanOrganizeMeetings(form.isCanOrganizeMeetings());
        Set<Capability> capabilities = form.getCapabilityIds() == null
                ? Set.of()
                : new LinkedHashSet<>(capabilityRepository.findAllById(form.getCapabilityIds()));
        role.setCapabilities(capabilities);
    }

    private void assertNotBuiltIn(AppRole role, String action) {
        if (role.isBuiltIn()) {
            throw new IllegalArgumentException("Role bawaan tidak bisa " + action);
        }
    }

    private void assertNameAvailable(String name, Long excludingId) {
        appRoleRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Nama role sudah dipakai");
                });
    }
}
