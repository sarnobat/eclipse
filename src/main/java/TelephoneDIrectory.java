import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * 
 * To store ~1m names
 */
public class TelephoneDIrectory {

	private final Map<String, Employee> _nameToEmployee = new HashMap<String, Employee>();
	private final Map<String, Employee> _phoneToEmployee = new HashMap<String, Employee>();
	
	private final TelephoneDIrectory instance = new TelephoneDIrectory();
	
	// private so only getInstance() can instantiate
	private TelephoneDIrectory() {
	}
	
	public TelephoneDIrectory getInstance() {
		return instance;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		final TelephoneDIrectory directory = new TelephoneDIrectory();
		
		new Thread() {
			@Override
			public void run() {
				directory.getEmployeeName("800-737-4");
			}
		}.start();
		

		new Thread() {
			@Override
			public void run() {
				directory.getEmployeeName("Daisy");
			}
		}.start();
		

		new Thread() {
			@Override
			public void run() {
				directory.getPhoneNumbers("Jim");
			}
		}.start();
		
	}
	
	// phone number -> employee name
	String getEmployeeName(String phoneNumber) {
		return _phoneToEmployee.get(phoneNumber).getName();
	}
	
	// employee name -> set of phone numbers
	Set<String> getPhoneNumbers(String employeeName) {
		return _nameToEmployee.get(employeeName).getPhoneNumbers();
	}

	
	private static class Employee {

		private final String employeeName = "";
		private final Set<String> _phoneNumbers = new HashSet<String>();
		
		public String getName() {
			return employeeName;
		}
		
		public Set<String> getPhoneNumbers() {
			return _phoneNumbers;
		}
		
		@Override
		public boolean equals(Object other) {
			
		}
		
		@Override
		public int hashCode() {
			
		}
	}
}
