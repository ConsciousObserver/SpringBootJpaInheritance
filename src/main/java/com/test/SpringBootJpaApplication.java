package com.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@SpringBootApplication
@RestController
@RequestMapping("/")
public class SpringBootJpaApplication {

	@Autowired
	private VisitRequestRepository visitRequestRepository;

	@Autowired
	private ContactPersonRepository contactPersonRepository;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootJpaApplication.class, args);
	}

	/**
	 * Creates 2 visitors with a visit request
	 * 
	 * Creates a contact without request to replace one visitor of previous
	 * request
	 */
	@PostConstruct
	public void init() {
		/*Save two visitors with one visit request*/
		VisitRequest visitRequest = new VisitRequest();
		visitRequest.getVisitors().add(new Visitor(visitRequest));
		visitRequest.getVisitors().add(new Visitor(visitRequest));

		visitRequestRepository.save(visitRequest);

		/*Create a contact person to exchange with one of the above visitors later*/
		contactPersonRepository.save(new ContactPerson(null, "9999999999"));
	}

	@GetMapping("/{visitRequestId}")
	public List<Visitor> getVisitors(@PathVariable long visitRequestId) {
		Optional<VisitRequest> visitRequestOpt = visitRequestRepository.findById(visitRequestId);

		if (!visitRequestOpt.isPresent()) {
			throw new RuntimeException(String.format("VisitRequest not found by ID: %d", visitRequestId));
		}

		return visitRequestOpt.get().getVisitors();
	}

	@GetMapping("/exchange/{visitRequestId}/{visitorIdToReplace}/{contactIdToExchange}")
	public List<Visitor> exchange(@PathVariable long visitRequestId, @PathVariable long visitorIdToReplace,
	        @PathVariable long contactIdToExchange) {

		VisitRequest visitRequest = validateVisitRequest(visitRequestId, visitorIdToReplace);
		Optional<ContactPerson> contactPersonOpt = contactPersonRepository.findById(contactIdToExchange);

		if (!contactPersonOpt.isPresent()) {
			throw new RuntimeException(String.format("Contact person not found by ID: %d", contactIdToExchange));
		}

		List<Visitor> visitors = visitRequest.getVisitors();

		/*Replace contact with visitor*/
		IntStream.range(0, visitors.size()).forEach(i -> {
			if (visitors.get(i).getId() == visitorIdToReplace) {
				ContactPerson contactPerson = contactPersonOpt.get();
				contactPerson.setVisitRequest(visitRequest);
				visitors.set(i, contactPerson);
			}
		});

		visitRequestRepository.save(visitRequest);

		return visitRequestRepository.findById(visitRequestId).get().getVisitors();
	}

	private VisitRequest validateVisitRequest(long visitRequestId, long visitorIdToReplace) {
		Optional<VisitRequest> visitRequestOpt = visitRequestRepository.findById(visitRequestId);

		if (!visitRequestOpt.isPresent()) {
			throw new RuntimeException(String.format("VisitRequest not found by ID: %d", visitRequestId));
		} else {
			if (visitRequestOpt.get()
			        .getVisitors()
			        .stream()
			        .noneMatch(visitor -> visitor.getId() == visitorIdToReplace)) {
				throw new RuntimeException(String.format("No visitor ID %d not found for visit request ID %d",
				        visitorIdToReplace, visitRequestId));
			}
		}
		return visitRequestOpt.get();
	}
}

interface VisitRequestRepository extends JpaRepository<VisitRequest, Long> {

}

interface ContactPersonRepository extends JpaRepository<ContactPerson, Long> {

}

@Data
@Entity(name = "request")
class VisitRequest {
	@Id
	@GeneratedValue
	private Long id;

	@OneToMany(mappedBy = "visitRequest", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<Visitor> visitors = new ArrayList<>();
}

@NoArgsConstructor
@Data
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
class Visitor {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne
	@JsonBackReference
	private VisitRequest visitRequest;

	public Visitor(VisitRequest visitRequest) {
		this.visitRequest = visitRequest;
	}
}

@NoArgsConstructor
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
class ContactPerson extends Visitor {
	private String phoneNumber;

	public ContactPerson(VisitRequest visitRequest, String phoneNumber) {
		super(visitRequest);
		this.phoneNumber = phoneNumber;
	}
}