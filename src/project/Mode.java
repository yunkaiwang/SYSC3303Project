package project;

public enum Mode {
	QUITE("QUITE"),
	VERBOSE("VERBOSE");
	
	String mode;
	
	private Mode(String mode) {
		this.mode = mode;
	}
	
	/**
	 * Getter for the mode string, used for printing current mode
	 * 
	 * @return
	 */
	public String mode() {
		return mode;
	}
	
	/**
	 * Switch the print mode, used when the user chooses to switch the mode
	 * 
	 * @return mode - the new mode
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
