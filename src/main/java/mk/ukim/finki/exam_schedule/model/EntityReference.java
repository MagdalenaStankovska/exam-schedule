package mk.ukim.finki.exam_schedule.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class EntityReference {

    private String referenceId;
    private String referenceClass;
}
