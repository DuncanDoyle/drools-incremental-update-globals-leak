package org.jboss.ddoyle.drools.demo.model.v1;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class AnotherEvent implements Event {
private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd:HHmmssSSS");
	
	/**
	 * SerialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	private final String id;
	
	private final long timestamp;
	
	private final String code; 
	
	public AnotherEvent(final String code, final Date eventTimestamp) {
		this(UUID.randomUUID().toString(), code, eventTimestamp);
	}
	
	public AnotherEvent(final String eventId, final String code, final Date eventTimestamp) {
		this(eventId, code, eventTimestamp.getTime());
	}
	
	public AnotherEvent(final String eventId, final String code, final long eventTimestamp) {
		this.id = eventId;
		this.code = code;
		this.timestamp = eventTimestamp;
	}
	
	
	
	public String getId() {
		return id;
	}
	
	public String getCode() {
		return code;
	}

	public Date getTimestamp() {
		return new Date(timestamp);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", id).append("timestamp", DATE_FORMAT.format(timestamp)).toString();
	}
}
