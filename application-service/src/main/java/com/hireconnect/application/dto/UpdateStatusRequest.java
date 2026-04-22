package com.hireconnect.application.dto;

import com.hireconnect.application.enums.ApplicationStatus;

public class UpdateStatusRequest {
	 private ApplicationStatus status;
	 public UpdateStatusRequest()
	 {
		 
	 }
	 public ApplicationStatus getStatus() {
		 return status;
	 }
	 public void setStatus(ApplicationStatus status) {
		 this.status = status;
	 }
}
