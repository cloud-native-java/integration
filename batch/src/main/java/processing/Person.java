package processing;


public class Person {
	private int age;
	private String firstName, email;

	public Person() {
	}

	public void setAge(int age) {
		this.age = age;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Person(int age, String firstName, String email) {
		this.age = age;
		this.firstName = firstName;
		this.email = email;
	}

	public int getAge() {
		return age;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getEmail() {
		return email;
	}
}
