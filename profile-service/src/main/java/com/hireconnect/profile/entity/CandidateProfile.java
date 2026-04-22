package com.hireconnect.profile.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    private String fullName;
    private String email;
    private String mobile;

    @ElementCollection
    @CollectionTable(
        name = "candidate_profile_skills",
        foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private List<String> skills;

    private int experience;
    private String resumeUrl;

    @ElementCollection
    @CollectionTable(
        name = "candidate_profile_addresses",
        foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    private List<Address> addresses;

    private String role = "CANDIDATE";

	public Long getProfileId() {
		return profileId;
	}

	public void setProfileId(Long profileId) {
		this.profileId = profileId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public List<String> getSkills() {
		return skills;
	}

	public void setSkills(List<String> skills) {
		this.skills = skills;
	}

	public int getExperience() {
		return experience;
	}

	public void setExperience(int experience) {
		this.experience = experience;
	}

	public String getResumeUrl() {
		return resumeUrl;
	}

	public void setResumeUrl(String resumeUrl) {
		this.resumeUrl = resumeUrl;
	}

	public List<Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<Address> addresses) {
		this.addresses = addresses;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	

    // Getters & Setters
}
