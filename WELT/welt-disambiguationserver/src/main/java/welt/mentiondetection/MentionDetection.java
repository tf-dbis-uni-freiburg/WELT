package welt.mentiondetection;

/**
 * 
 * @author Dimitar
 * 
 * Interface for the Mention Detection step. This interface is used to make the 
 * MentionDetection step pluggable and interchangeable.
 *
 */
public interface MentionDetection {

	/**
	 * @return String - An XML like string with the mention detections.
	 */
	public String detectMentions();
	
}
