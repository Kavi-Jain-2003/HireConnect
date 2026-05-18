package com.hireconnect.job.service;

// ════════════════════════════════════════════════════════════════
//  JobServiceImpl — Unit Tests
//  Framework : JUnit 5 + Mockito
//  Kya test kar rahe hain:
//    1. addJob()      — job successfully save hoti hai
//    2. getAllJobs()  — sab jobs return hoti hain
//    3. getJobById() — sahi job return hoti hai / not found error
//    4. searchJobs() — title/location/category se filter hota hai
//    5. deleteJob()  — job delete hoti hai / unauthorized check
//    6. updateJob()  — job update hoti hai
//    7. pauseJob()   — status PAUSED hota hai
//    8. closeJob()   — status CLOSED hota hai
// ════════════════════════════════════════════════════════════════

import com.hireconnect.job.client.AuthClient;
import com.hireconnect.job.client.NotificationClient;
import com.hireconnect.job.client.ProfileClient;
import com.hireconnect.job.dto.JobRequest;
import com.hireconnect.job.entity.Job;
import com.hireconnect.job.exception.ResourceNotFoundException;
import com.hireconnect.job.repository.JobRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) — Mockito ko JUnit 5 ke saath use karne ke liye
@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    // ── Mocks ─────────────────────────────────────────────────────
    // @Mock matlab — yeh real class nahi hai, sirf nakli (fake) object hai
    // Taki real DB ya HTTP calls na ho — sirf logic test ho
    @Mock
    private JobRepository jobRepository;

    @Mock
    private ProfileClient profileClient;

    @Mock
    private AuthClient authClient;

    @Mock
    private NotificationClient notificationClient;

    // @InjectMocks — yeh real class hai jisko hum test kar rahe hain
    // Upar wale @Mock objects automatically inject ho jaate hain isme
    @InjectMocks
    private JobServiceImpl jobService;

    // ── Test Data ─────────────────────────────────────────────────
    // Yeh objects har test se pehle bante hain (@BeforeEach)
    private Job sampleJob;
    private JobRequest sampleRequest;

    // @BeforeEach — har @Test se PEHLE yeh method run hota hai
    // Sample data prepare karte hain yahan
    @BeforeEach
    void setUp() {
        // Sample Job entity — jaise DB mein hoti hai
        sampleJob = new Job();
        sampleJob.setJobId(1L);
        sampleJob.setTitle("Java Developer");
        sampleJob.setLocation("Agra");
        sampleJob.setCategory("IT");
        sampleJob.setType("Full-time");
        sampleJob.setSalaryMin(300000.0);
        sampleJob.setSalaryMax(600000.0);
        sampleJob.setExperienceRequired(2);
        sampleJob.setCompany("TechCorp");
        sampleJob.setPostedBy("recruiter1@gmail.com");
        sampleJob.setStatus("OPEN");
        sampleJob.setSkills(Arrays.asList("Java", "Spring Boot"));

        // Sample JobRequest — jaise frontend se aata hai
        sampleRequest = new JobRequest();
        sampleRequest.setTitle("Java Developer");
        sampleRequest.setLocation("Agra");
        sampleRequest.setCategory("IT");
        sampleRequest.setType("Full-time");
        sampleRequest.setSalaryMin(300000.0);
        sampleRequest.setSalaryMax(600000.0);
        sampleRequest.setExperienceRequired(2);
        sampleRequest.setDescription("Looking for Java developer");
        sampleRequest.setCompany("TechCorp");
        sampleRequest.setSkills(Arrays.asList("Java", "Spring Boot"));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 1 — addJob()
    // Kya test kar rahe hain: Job successfully save hoti hai
    // Expected: "Job created successfully" return hona chahiye
    // ════════════════════════════════════════════════════════════
    @Test
    void addJob_ShouldSaveJobAndReturnSuccessMessage() {
        // ARRANGE — Mock setup karo
        // jobRepository.save() ko call karein toh sampleJob return karo
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        // Candidate notification ke liye profileClient mock
        when(profileClient.getAllCandidates()).thenReturn(null);

        // ACT — Actual method call karo
        String result = jobService.addJob(sampleRequest, "recruiter1@gmail.com", 1L);

        // ASSERT — Result check karo
        assertEquals("Job created successfully", result);

        // Verify — jobRepository.save() exactly 1 baar call hua
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 2 — getAllJobs()
    // Kya test kar rahe hain: Sab jobs return hoti hain
    // Expected: 2 jobs ki list return honi chahiye
    // ════════════════════════════════════════════════════════════
    @Test
    void getAllJobs_ShouldReturnAllJobs() {
        // ARRANGE
        Job job2 = new Job();
        job2.setJobId(2L);
        job2.setTitle("Frontend Developer");
        job2.setPostedBy("recruiter2@gmail.com");

        List<Job> jobList = Arrays.asList(sampleJob, job2);

        // jobRepository.findAll() call ho toh 2 jobs return karo
        when(jobRepository.findAll()).thenReturn(jobList);

        // ACT
        List<Job> result = jobService.getAllJobs();

        // ASSERT
        assertNotNull(result);                    // result null nahi hona chahiye
        assertEquals(2, result.size());           // exactly 2 jobs honi chahiye
        assertEquals("Java Developer", result.get(0).getTitle()); // pehli job ka title check
    }

    // ════════════════════════════════════════════════════════════
    // TEST 3 — getAllJobs() empty list
    // Kya test kar rahe hain: Agar koi job nahi toh empty list aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void getAllJobs_WhenNoJobs_ShouldReturnEmptyList() {
        // ARRANGE — empty list return karo
        when(jobRepository.findAll()).thenReturn(Collections.emptyList());

        // ACT
        List<Job> result = jobService.getAllJobs();

        // ASSERT
        assertNotNull(result);
        assertTrue(result.isEmpty()); // list empty honi chahiye
    }

    // ════════════════════════════════════════════════════════════
    // TEST 4 — getJobById() — job mili
    // Kya test kar rahe hain: ID se job milti hai
    // Expected: sampleJob return hona chahiye
    // ════════════════════════════════════════════════════════════
    @Test
    void getJobById_WhenJobExists_ShouldReturnJob() {
        // ARRANGE
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        // ACT
        Job result = jobService.getJobById(1L);

        // ASSERT
        assertNotNull(result);
        assertEquals(1L, result.getJobId());
        assertEquals("Java Developer", result.getTitle());
        assertEquals("Agra", result.getLocation());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 5 — getJobById() — job nahi mili
    // Kya test kar rahe hain: Wrong ID pe exception aata hai
    // Expected: ResourceNotFoundException throw honi chahiye
    // ════════════════════════════════════════════════════════════
    @Test
    void getJobById_WhenJobNotFound_ShouldThrowException() {
        // ARRANGE — 999L ID ke liye koi job nahi
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        // ASSERT + ACT — Exception aani chahiye
        assertThrows(ResourceNotFoundException.class, () -> {
            jobService.getJobById(999L);
        });
    }

    // ════════════════════════════════════════════════════════════
    // TEST 6 — searchJobs() — title se search
    // Kya test kar rahe hain: "Java" title se search karo
    // Expected: sirf Java Developer wali job aaye
    // ════════════════════════════════════════════════════════════
    @Test
    void searchJobs_ByTitle_ShouldReturnMatchingJobs() {
        // ARRANGE — 2 jobs hain DB mein
        Job job2 = new Job();
        job2.setJobId(2L);
        job2.setTitle("Frontend Developer");
        job2.setLocation("Pune");
        job2.setCategory("IT");
        job2.setSalaryMin(200000.0);
        job2.setSalaryMax(400000.0);
        job2.setExperienceRequired(1);

        when(jobRepository.findAll()).thenReturn(Arrays.asList(sampleJob, job2));

        // ACT — "Java" se search karo
        List<Job> result = jobService.searchJobs("Java", null, null, null, null, null);

        // ASSERT — sirf 1 job milni chahiye
        assertEquals(1, result.size());
        assertEquals("Java Developer", result.get(0).getTitle());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 7 — searchJobs() — location se search
    // Kya test kar rahe hain: "Agra" location se search karo
    // ════════════════════════════════════════════════════════════
    @Test
    void searchJobs_ByLocation_ShouldReturnMatchingJobs() {
        // ARRANGE
        Job job2 = new Job();
        job2.setJobId(2L);
        job2.setTitle("Frontend Developer");
        job2.setLocation("Pune");
        job2.setCategory("IT");
        job2.setSalaryMin(200000.0);
        job2.setSalaryMax(400000.0);
        job2.setExperienceRequired(1);

        when(jobRepository.findAll()).thenReturn(Arrays.asList(sampleJob, job2));

        // ACT — "Agra" se search karo
        List<Job> result = jobService.searchJobs(null, "Agra", null, null, null, null);

        // ASSERT — sirf Agra wali job milni chahiye
        assertEquals(1, result.size());
        assertEquals("Agra", result.get(0).getLocation());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 8 — searchJobs() — no match
    // Kya test kar rahe hain: Koi match nahi toh empty list
    // ════════════════════════════════════════════════════════════
    @Test
    void searchJobs_WhenNoMatch_ShouldReturnEmptyList() {
        // ARRANGE
        when(jobRepository.findAll()).thenReturn(Arrays.asList(sampleJob));

        // ACT — "Python" se search karo — koi Java job match nahi karegi
        List<Job> result = jobService.searchJobs("Python", null, null, null, null, null);

        // ASSERT
        assertTrue(result.isEmpty());
    }

    // ════════════════════════════════════════════════════════════
    // TEST 9 — deleteJob() — successful delete
    // Kya test kar rahe hain: Owner apni job delete kar sakta hai
    // ════════════════════════════════════════════════════════════
    @Test
    void deleteJob_WhenOwner_ShouldDeleteSuccessfully() {
        // ARRANGE
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        // ACT — recruiter1@gmail.com owner hai
        String result = jobService.deleteJob(1L, "recruiter1@gmail.com");

        // ASSERT
        assertEquals("Job deleted", result);
        verify(jobRepository, times(1)).delete(sampleJob); // delete call hua
    }

    // ════════════════════════════════════════════════════════════
    // TEST 10 — deleteJob() — unauthorized
    // Kya test kar rahe hain: Dusra recruiter delete nahi kar sakta
    // Expected: RuntimeException throw honi chahiye
    // ════════════════════════════════════════════════════════════
    @Test
    void deleteJob_WhenNotOwner_ShouldThrowException() {
        // ARRANGE
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));

        // ACT + ASSERT — dusra recruiter delete karne ki koshish kare
        assertThrows(RuntimeException.class, () -> {
            jobService.deleteJob(1L, "recruiter2@gmail.com"); // wrong owner
        });

        // Verify — delete call NAHI hona chahiye
        verify(jobRepository, never()).delete(any(Job.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 11 — updateJob() — successful update
    // Kya test kar rahe hain: Owner apni job update kar sakta hai
    // ════════════════════════════════════════════════════════════
    @Test
    void updateJob_WhenOwner_ShouldUpdateSuccessfully() {
        // ARRANGE
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        // New request with updated title
        sampleRequest.setTitle("Senior Java Developer");

        // ACT
        String result = jobService.updateJob(1L, sampleRequest, "recruiter1@gmail.com");

        // ASSERT
        assertEquals("Job updated", result);
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    // ════════════════════════════════════════════════════════════
    // TEST 12 — pauseJob()
    // Kya test kar rahe hain: Job ka status PAUSED ho jaaye
    // ════════════════════════════════════════════════════════════
    @Test
    void pauseJob_WhenOwner_ShouldSetStatusToPaused() {
        // ARRANGE
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        // ACT
        String result = jobService.pauseJob(1L, "recruiter1@gmail.com");

        // ASSERT
        assertEquals("Job paused", result);
        assertEquals("PAUSED", sampleJob.getStatus()); // status change hua
    }

    // ════════════════════════════════════════════════════════════
    // TEST 13 — closeJob()
    // Kya test kar rahe hain: Job ka status CLOSED ho jaaye
    // ════════════════════════════════════════════════════════════
    @Test
    void closeJob_WhenOwner_ShouldSetStatusToClosed() {
        // ARRANGE
        when(jobRepository.findById(1L)).thenReturn(Optional.of(sampleJob));
        when(jobRepository.save(any(Job.class))).thenReturn(sampleJob);

        // ACT
        String result = jobService.closeJob(1L, "recruiter1@gmail.com");

        // ASSERT
        assertEquals("Job closed", result);
        assertEquals("CLOSED", sampleJob.getStatus()); // status change hua
    }
}
