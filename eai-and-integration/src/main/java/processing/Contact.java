package processing;

public class Contact {

	private String fullName, email;

	public Contact() {
	}

	@Override
	public String toString() {
		return "Contact{" +
				"fullName='" + fullName + '\'' +
				", email='" + email + '\'' +
				", id=" + id +
				'}';
	}

	public Contact(String fullName, String email, long id) {
		this.fullName = fullName;
		this.email = email;
		this.id = id;
	}

	private long id;

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
