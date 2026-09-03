package id.jagr.rapat.division;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DivisionService {

    private final DivisionRepository divisionRepository;

    public List<Division> findAll() {
        return divisionRepository.findAll();
    }

    public Division findById(Long id) {
        return divisionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Divisi tidak ditemukan"));
    }

    @Transactional
    public Division create(String name) {
        assertNameAvailable(name, null);
        return divisionRepository.save(new Division(name));
    }

    @Transactional
    public Division rename(Long id, String name) {
        assertNameAvailable(name, id);
        Division division = findById(id);
        division.setName(name);
        return divisionRepository.save(division);
    }

    /** Called by the user module when an Admin toggles "jadikan ketua divisi" on a User. */
    @Transactional
    public void setKetua(Long divisionId, Long userId) {
        Division division = findById(divisionId);
        division.setKetuaDivisiUserId(userId);
        divisionRepository.save(division);
    }

    @Transactional
    public void clearKetuaIfMatches(Long divisionId, Long userId) {
        Division division = findById(divisionId);
        if (userId.equals(division.getKetuaDivisiUserId())) {
            division.setKetuaDivisiUserId(null);
            divisionRepository.save(division);
        }
    }

    private void assertNameAvailable(String name, Long excludingId) {
        divisionRepository.findByNameIgnoreCase(name)
                .filter(d -> !d.getId().equals(excludingId))
                .ifPresent(d -> {
                    throw new IllegalArgumentException("Nama divisi sudah dipakai");
                });
    }
}
