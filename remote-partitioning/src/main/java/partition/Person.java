package partition;


public class Person {
	private int id;
	private int age;
	private String firstName, email;

	public Person() {
	}

	public Person(int age, String firstName, String email) {
		this.age = age;
		this.firstName = firstName;
		this.email = email;
	}

	public Person(int id, int age, String firstName, String email) {
		this.id = id;
		this.age = age;
		this.firstName = firstName;
		this.email = email;
	}

	public int getId() {

		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
