package com.example.vibe1.user;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.vibe1.division.Division;
import com.example.vibe1.division.DivisionRepository;
import com.example.vibe1.division.DivisionService;
import com.example.vibe1.user.web.UserForm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DivisionRepository divisionRepository;
    private final DivisionService divisionService;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pengguna tidak ditemukan"));
    }

    @Transactional
    public User create(UserForm form) {
        validate(form, null);
        User user = new User();
        applyForm(user, form);
        user = userRepository.save(user);
        applyLeadership(user, form);
        return user;
    }

    @Transactional
    public User update(Long id, UserForm form) {
        User user = findById(id);
        validate(form, id);
        if (user.getDivision() != null) {
            divisionService.clearKetuaIfMatches(user.getDivision().getId(), id);
        }
        applyForm(user, form);
        user = userRepository.save(user);
        applyLeadership(user, form);
        return user;
    }

    private void validate(UserForm form, Long excludingId) {
        userRepository.findByEmailIgnoreCase(form.getEmail())
                .filter(existing -> !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email sudah dipakai");
                });

        boolean needsDivision = form.getRole() == Role.KETUA_DIVISI || form.getRole() == Role.KARYAWAN;
        if (needsDivision && form.getDivisionId() == null) {
            throw new IllegalArgumentException("Ketua Divisi/Karyawan wajib punya divisi");
        }
        if (!needsDivision && form.getDivisionId() != null) {
            throw new IllegalArgumentException("Admin/Direktur tidak boleh terikat ke satu divisi");
        }
        if (form.isDivisionLeader() && form.getRole() != Role.KETUA_DIVISI) {
            throw new IllegalArgumentException("Hanya Ketua Divisi yang bisa jadi pemimpin divisi");
        }
    }

    private void applyForm(User user, UserForm form) {
        user.setEmail(form.getEmail());
        user.setFullName(form.getFullName());
        user.setRole(form.getRole());
        user.setEnabled(form.isEnabled());
        if (form.getDivisionId() != null) {
            Division division = divisionRepository.findById(form.getDivisionId())
                    .orElseThrow(() -> new IllegalArgumentException("Divisi tidak ditemukan"));
            user.setDivision(division);
        } else {
            user.setDivision(null);
        }
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        }
    }

    private void applyLeadership(User user, UserForm form) {
        if (user.getDivision() == null) {
            return;
        }
        if (form.isDivisionLeader()) {
            divisionService.setKetua(user.getDivision().getId(), user.getId());
        } else {
            divisionService.clearKetuaIfMatches(user.getDivision().getId(), user.getId());
        }
    }
}
