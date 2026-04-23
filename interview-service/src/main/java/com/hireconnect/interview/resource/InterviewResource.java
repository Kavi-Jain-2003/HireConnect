package com.hireconnect.interview.resource;

import com.hireconnect.interview.entity.Interview;
import com.hireconnect.interview.service.InterviewService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/interviews")
public class InterviewResource {

    @Autowired
    private InterviewService service;

    // ✅ Recruiter only
    @PostMapping
    public Object schedule(@RequestBody Interview interview,
                           HttpServletRequest request) {

        String role = (String) request.getAttribute("role");

        if (!"RECRUITER".equals(role)) {
            return "Access Denied: Only Recruiter can schedule interviews";
        }

        return service.scheduleInterview(interview);
    }

    // ✅ Candidate only
    @PutMapping("/{id}/confirm")
    public Object confirm(@PathVariable int id,
                          HttpServletRequest request) {

        String role = (String) request.getAttribute("role");

        if (!"CANDIDATE".equals(role)) {
            return "Access Denied: Only Candidate can confirm interview";
        }

        return service.confirmInterview(id);
    }

    // ✅ Candidate only
    @PutMapping("/{id}/reschedule")
    public Object reschedule(@PathVariable int id,
                             @RequestParam String time,
                             HttpServletRequest request) {

        String role = (String) request.getAttribute("role");

        if (!"CANDIDATE".equals(role)) {
            return "Access Denied: Only Candidate can reschedule";
        }

        LocalDateTime newTime = LocalDateTime.parse(time);
        return service.rescheduleInterview(id, newTime);
    }

    // ✅ Recruiter only
    @PutMapping("/{id}/cancel")
    public Object cancel(@PathVariable int id,
                         HttpServletRequest request) {

        String role = (String) request.getAttribute("role");

        if (!"RECRUITER".equals(role)) {
            return "Access Denied: Only Recruiter can cancel";
        }

        service.cancelInterview(id);
        return "Interview Cancelled Successfully";
    }

    @GetMapping("/application/{appId}")
    public List<Interview> getByApplication(@PathVariable int appId) {
        return service.getByApplication(appId);
    }

    @GetMapping("/status/{status}")
    public List<Interview> getByStatus(@PathVariable String status) {
        return service.getByStatus(status);
    }
}
