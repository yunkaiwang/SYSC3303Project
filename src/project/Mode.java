package project;

public enum Mode {
	QUITE("QUITE"),
	VERBOSE("VERBOSE");
	
	String mode; // string representation of the mode
	
	/**
	 * Constructor
	 * 
	 * @param mode
	 */
	private Mode(String mode) {
		this.mode = mode;
	}
	
	/**
	 * Getter
	 * 
	 * @return mode
	 */
	public String mode() {
		return mode;
	}
	
	/**
	 * Switch the print mode, used when the user chooses to switch the mode
	 * 
	 * @return newMode
	 */
	public Mode switchMode() {
		switch (this) {
		case QUITE:
			return VERBOSE;
		case VERBOSE:
			return QUITE;
		default: // default mode
			return QUITE;
		}
	}
}
