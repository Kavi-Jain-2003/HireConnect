package com.hireconnect.profile.service;

// ════════════════════════════════════════════════════════════════
//  ProfileServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1.  addCandidateProfile()       — successful save
//    2.  addRecruiterProfile()       — successful save
//    3.  getCandidateByEmail()       — found ✅
//    4.  getCandidateByEmail()       — not found ❌
//    5.  getCandidateById()          — found ✅
//    6.  getCandidateById()          — not found ❌
//    7.  getRecruiterByEmail()       — found ✅
//    8.  getRecruiterByEmail()       — not found ❌
//    9.  updateCandidateProfile()    — successful update ✅
//    10. updateCandidateProfile()    — unauthorized ❌
//    11. updateRecruiterProfile()    — successful update ✅
//    12. updateRecruiterProfile()    — unauthorized ❌
//    13. deleteCandidateProfile()    — successful delete ✅
//    14. deleteCandidateProfile()    — unauthorized ❌
//    15. getAllCandidates()          — list return ✅
//    16. getAllRecruiters()          — list return ✅
// ════════════════════════════════════════════════════════════════

import com.hireconnect.profile.entity.CandidateProfile;
import com.hireconnect.profile.entity.RecruiterProfile;
import com.hireconnect.profile.repository.CandidateProfileRepository;
import com.hireconnect.profile.repository.RecruiterProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    @Mock
    private CandidateProfileRepository candidateRepo;

    @Mock
    private RecruiterProfileRepository recruiterRepo;

    // ── Real class ────────────────────────────────────────────────
    @InjectMocks
    private ProfileServiceImpl profileService;

    // ── Test Data ─────────────────────────────────────────────────
    private CandidateProfile sampleCandidate;
    private RecruiterProfile sampleRecruiter;

    @BeforeEach
    void setUp() {
        // Sample Candidate Profile
        sampleCandidate = new CandidateProfile();
        sampleCandidate.setProfileId(1L);
        sampleCandidate.setUserId(3L);
        sampleCandidate.setFullName("Kavi Jain");
        sampleCandidate.setEmail("candidate1@gmail.com");
        sampleCandidate.setMobile("9876543210");
        sampleCandidate.setExperience(2);
        sampleCandidate.setSkills(Arrays.asList("Java", "Spring Boot"));

        // Sample Recruiter Profile
        sampleRecruiter = new RecruiterProfile();
        sampleRecruiter.setProfileId(2L);
        sampleRecruiter.setFullName("Pooja Sharma");
        sampleRecruiter.setEmail("recruiter1@gmail.com");
        sampleRecruiter.setCompanyName("TechCorp");
        sampleRecruiter.setIndustry("IT");
        sampleRecruiter.setCompanySize("50-200");
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — addCandidateProfile() ✅
    // Kya test: Candidate profile successfully save ho
    // ════════════════════════════════════════════════════════════
    @Test
    void addCandidateProfile_ShouldSaveAndReturn() {
        // ARRANGE
        when(candidateRepo.save(any(CandidateProfile.class))).thenReturn(sampleCandidate);

        // ACT
        CandidateProfile result = profileService.addCandidateProfile(sampleCandidate);

        // ASSERT
        assertNotNull(result);
        assertEquals("Kavi Jain", result.getFullName());
        assertEquals("candidate1@gmail.com", result.getEmail());
        verify(candidateRepo, times(1)).save(sampleCandidate);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — addRecruiterProfile() ✅
    // Kya test: Recruiter profile successfully save ho
    // ════════════════════════════════════════════════════════════
    @Test
    void addRecruiterProfile_ShouldSaveAndReturn() {
        // ARRANGE
        when(recruiterRepo.save(any(RecruiterProfile.class))).thenReturn(sampleRecruiter);

        // ACT
        RecruiterProfile result = profileService.addRecruiterProfile(sampleRecruiter);

        // ASSERT
        assertNotNull(result);
        assertEquals("Pooja Sharma", result.getFullName());
        assertEquals("TechCorp", result.getCompanyName());
        verify(recruiterRepo, times(1)).save(sampleRecruiter);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — getCandidateByEmail() — Found ✅
    // Kya test: Email se candidate profile milti hai
    // ════════════════════════════════════════════════════════════
    @Test
    void getCandidateByEmail_WhenExists_ShouldReturn() {
        // ARRANGE
        when(candidateRepo.findByEmail("candidate1@gmail.com"))
                .thenReturn(Optional.of(sampleCandidate));

        // ACT
        CandidateProfile result = profileService.getCandidateByEmail("candidate1@gmail.com");

        // ASSERT
        assertNotNull(result);
        assertEquals("Kavi Jain", result.getFullName());
        assertEquals("candidate1@gmail.com", result.getEmail());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — getCandidateByEmail() — Not Found ❌
    // Kya test: Wrong email pe exception aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void getCandidateByEmail_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(candidateRepo.findByEmail("wrong@gmail.com"))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            profileService.getCandidateByEmail("wrong@gmail.com");
        });

        assertEquals("Candidate not found", ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — getCandidateById() — Found ✅
    // ════════════════════════════════════════════════════════════
    @Test
    void getCandidateById_WhenExists_ShouldReturn() {
        // ARRANGE
        when(candidateRepo.findById(1L)).thenReturn(Optional.of(sampleCandidate));

        // ACT
        CandidateProfile result = profileService.getCandidateById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getProfileId());
        assertEquals("Kavi Jain", result.getFullName());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — getCandidateById() — Not Found ❌
    // ════════════════════════════════════════════════════════════
    @Test
    void getCandidateById_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(candidateRepo.findById(999L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThrows(RuntimeException.class, () -> {
            profileService.getCandidateById(999L);
        });
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — getRecruiterByEmail() — Found ✅
    // ════════════════════════════════════════════════════════════
    @Test
    void getRecruiterByEmail_WhenExists_ShouldReturn() {
        // ARRANGE
        when(recruiterRepo.findByEmail("recruiter1@gmail.com"))
                .thenReturn(Optional.of(sampleRecruiter));

        // ACT
        RecruiterProfile result = profileService.getRecruiterByEmail("recruiter1@gmail.com");

        // ASSERT
        assertNotNull(result);
        assertEquals("Pooja Sharma", result.getFullName());
        assertEquals("TechCorp", result.getCompanyName());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — getRecruiterByEmail() — Not Found ❌
    // ════════════════════════════════════════════════════════════
    @Test
    void getRecruiterByEmail_WhenNotFound_ShouldThrowException() {
        // ARRANGE
        when(recruiterRepo.findByEmail("wrong@gmail.com"))
                .thenReturn(Optional.empty());

        // ACT + ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            profileService.getRecruiterByEmail("wrong@gmail.com");
        });

        assertEquals("Recruiter not found", ex.getMessage());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — updateCandidateProfile() — Successful ✅
    // Kya test: Owner apna profile update kar sake
    // ════════════════════════════════════════════════════════════
    @Test
    void updateCandidateProfile_WhenOwner_ShouldUpdate() {
        // ARRANGE
        when(candidateRepo.findById(1L)).thenReturn(Optional.of(sampleCandidate));

        CandidateProfile updatedProfile = new CandidateProfile();
        updatedProfile.setFullName("Kavi Jain Updated");
        updatedProfile.setMobile("9999999999");
        updatedProfile.setExperience(3);

        when(candidateRepo.save(any(CandidateProfile.class))).thenReturn(updatedProfile);

        // ACT
        CandidateProfile result = profileService.updateCandidateProfile(
                1L, updatedProfile, "candidate1@gmail.com");

        // ASSERT
        assertNotNull(result);
        verify(candidateRepo, times(1)).save(any(CandidateProfile.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — updateCandidateProfile() — Unauthorized ❌
    // Kya test: Dusra user update nahi kar sakta
    // ════════════════════════════════════════════════════════════
    @Test
    void updateCandidateProfile_WhenNotOwner_ShouldThrowException() {
        // ARRANGE
        when(candidateRepo.findById(1L)).thenReturn(Optional.of(sampleCandidate));

        CandidateProfile updatedProfile = new CandidateProfile();

        // ACT + ASSERT — dusra email se update karne ki koshish
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            profileService.updateCandidateProfile(
                    1L, updatedProfile, "hacker@gmail.com");
        });

        assertEquals("Unauthorized access", ex.getMessage());
        verify(candidateRepo, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 11 — updateRecruiterProfile() — Successful ✅
    // ════════════════════════════════════════════════════════════
    @Test
    void updateRecruiterProfile_WhenOwner_ShouldUpdate() {
        // ARRANGE
        when(recruiterRepo.findById(2L)).thenReturn(Optional.of(sampleRecruiter));

        RecruiterProfile updated = new RecruiterProfile();
        updated.setFullName("Pooja Updated");
        updated.setCompanyName("NewTechCorp");

        when(recruiterRepo.save(any(RecruiterProfile.class))).thenReturn(updated);

        // ACT
        RecruiterProfile result = profileService.updateRecruiterProfile(
                2L, updated, "recruiter1@gmail.com");

        // ASSERT
        assertNotNull(result);
        verify(recruiterRepo, times(1)).save(any(RecruiterProfile.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 12 — updateRecruiterProfile() — Unauthorized ❌
    // ════════════════════════════════════════════════════════════
    @Test
    void updateRecruiterProfile_WhenNotOwner_ShouldThrowException() {
        // ARRANGE
        when(recruiterRepo.findById(2L)).thenReturn(Optional.of(sampleRecruiter));

        RecruiterProfile updated = new RecruiterProfile();

        // ACT + ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            profileService.updateRecruiterProfile(
                    2L, updated, "hacker@gmail.com");
        });

        assertEquals("Unauthorized access", ex.getMessage());
        verify(recruiterRepo, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 13 — deleteCandidateProfile() — Successful ✅
    // ════════════════════════════════════════════════════════════
    @Test
    void deleteCandidateProfile_WhenOwner_ShouldDelete() {
        // ARRANGE
        when(candidateRepo.findById(1L)).thenReturn(Optional.of(sampleCandidate));

        // ACT
        profileService.deleteCandidateProfile(1L, "candidate1@gmail.com");

        // ASSERT
        verify(candidateRepo, times(1)).delete(sampleCandidate);
    }

    // ════════════════════════════════════════════════════════════
    // TEST 14 — deleteCandidateProfile() — Unauthorized ❌
    // ════════════════════════════════════════════════════════════
    @Test
    void deleteCandidateProfile_WhenNotOwner_ShouldThrowException() {
        // ARRANGE
        when(candidateRepo.findById(1L)).thenReturn(Optional.of(sampleCandidate));

        // ACT + ASSERT
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            profileService.deleteCandidateProfile(1L, "hacker@gmail.com");
        });

        assertEquals("Unauthorized access", ex.getMessage());
        verify(candidateRepo, never()).delete(any());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 15 — getAllCandidates() ✅
    // Kya test: Saare candidates return hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getAllCandidates_ShouldReturnAllCandidates() {
        // ARRANGE
        CandidateProfile c2 = new CandidateProfile();
        c2.setProfileId(2L);
        c2.setFullName("Rahul Kumar");
        c2.setEmail("rahul@gmail.com");

        when(candidateRepo.findAll()).thenReturn(Arrays.asList(sampleCandidate, c2));

        // ACT
        List<CandidateProfile> result = profileService.getAllCandidates();

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Kavi Jain", result.get(0).getFullName());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 16 — getAllRecruiters() ✅
    // Kya test: Saare recruiters return hon
    // ════════════════════════════════════════════════════════════
    @Test
    void getAllRecruiters_ShouldReturnAllRecruiters() {
        // ARRANGE
        RecruiterProfile r2 = new RecruiterProfile();
        r2.setProfileId(3L);
        r2.setFullName("Amit Singh");
        r2.setEmail("recruiter2@gmail.com");
        r2.setCompanyName("AcmeCorp");

        when(recruiterRepo.findAll()).thenReturn(Arrays.asList(sampleRecruiter, r2));

        // ACT
        List<RecruiterProfile> result = profileService.getAllRecruiters();

        // ASSERT
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Pooja Sharma", result.get(0).getFullName());
    }
}
