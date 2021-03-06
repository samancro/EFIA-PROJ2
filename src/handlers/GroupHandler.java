package handlers;

import java.io.IOException;
import java.util.ArrayList;

import org.lightcouch.NoDocumentException;

import data.Group;
import data.Membership;
import data.Message;
import exceptions.CustomException;

public class GroupHandler {
	DBHandler dbHandler;
	String localServerAddress;

	public GroupHandler(String localServerAddress) {
		dbHandler = new DBHandler();
		this.localServerAddress = localServerAddress;
	}

	/**
	 * @param groupName
	 * @param owner
	 * @throw CustomException In case the group name is repeated for that owner
	 */
	public void addNewGroup(String groupName, String owner) 
			throws CustomException {
		Group group = new Group(groupName, owner);
		ArrayList<Group> groups = dbHandler.getGroupsByOwner(owner);
		if (groups.contains(group)) {
			throw new CustomException(CustomException.GROUP_NAME_UNAVAILABLE);
		}
		dbHandler.addNewGroup(group);
	}
	
	/**
	 * 
	 * @param email
	 */
	public ArrayList<Group> getGroups(String email) {
		return dbHandler.getGroups(email);
	}

	// TODO Implement the invitation token logic
	/**
	 * 
	 * @param groupID
	 * @param newUserEmail
	 * 
	 */
	public void addNewUserToGroup(String groupID, String newMemberEmail, String ownerEmail) throws CustomException {
		try {
			Membership newMember = new Membership(newMemberEmail,
					SecureGen.generateSecureString(32));
			if (!dbHandler.ifGroupExists(groupID))
				throw new CustomException("Group", "The group ID is wrong");
			if(!dbHandler.ifUserExists(newMemberEmail))
				throw new CustomException("Group", "There is no such user");

			Group group = dbHandler.getGroup(groupID);
			if(!group.getOwner().equalsIgnoreCase(ownerEmail))
				throw new CustomException("Group","Unauthorized, you should be admin");
			ArrayList<Membership> members = group.getUsers();

			boolean userIsAMember = false;
			for (Membership member : members) {
				if (member.getEmail().equalsIgnoreCase(newMemberEmail)) {
					userIsAMember = true;
					newMember = member;
					if (member.getToken() == "")
						throw new CustomException("Group", "The member has already confirmed");
				}
			}
			if (!userIsAMember)
				members.add(newMember);
			else
				throw new CustomException("Group", "The user is already in that group");

			dbHandler.updateGroup(group);

			sendGroupMembershipConfirmationLink(group, newMember);
		} catch (Exception e) {
			throw new CustomException(CustomException.INTERNAL_ERROR, e.getMessage());
		}
	}

	private void sendGroupMembershipConfirmationLink(Group group,
			Membership member) throws IOException {
		String confirmMessage = "Please confirm your membership in \""
				+ group.getName() + "\" group owned by \"" + group.getOwner()
				+ "\"  by clicking on following link: \n" + "<a href=\"http://"
				+ localServerAddress + ":8080/proj2/GroupServlet?method=confirm&group_id="
				+ group.get_id() + "&token=" + member.getToken() + "&email="
				+ member.getEmail() + "\">Click me!</a>";
		System.out.println(confirmMessage);
		EmailHandler emailHandler = new EmailHandler(member.getEmail(),
				"Group Invitation", confirmMessage);
		emailHandler.start();
	}

	/**
	 * @throws CustomException 
	 */
	public int confirmNewUser(String groupID, String userEmail, String token) throws CustomException {
		if (!dbHandler.ifGroupExists(groupID))
			throw new CustomException("Group", "The group ID is wrong");
		Group group = dbHandler.getGroup(groupID);
		ArrayList<Membership> members = group.getUsers();

		for (Membership member : members) {
			if (member.getEmail().equalsIgnoreCase(userEmail)) {
				if (member.getToken().equalsIgnoreCase(token)){
					members.remove(member);
					member.setToken("");
					members.add(member);
					group.setUsers(members);
					dbHandler.updateGroup(group);
					return 0;
				}else
					throw new CustomException("Group", "Either confirmed before or the token is wrong");
			}
		}
		throw new CustomException("Group", "The user hasn't been invited to the group");
	}

	/**
	 * 
	 * @param groupID
	 * @param newMessage
	 * @throws CustomException 
	 */
	public void addNewMessageToGroup(String groupID, Message newMessage) throws CustomException {
		Group group = dbHandler.getGroup(groupID);
		
		if(!userIsMember(newMessage.getUser(), group))
			throw new CustomException("Group", "The user doesn't have permission to post");
		
		group.getMessages().add(newMessage);
		dbHandler.updateGroup(group);
	}
	
	public boolean userIsMember(String user, Group group) {
		boolean ifUserIsAMember = false;
		for(Membership member : group.getUsers()) {
			if(member.getEmail().equalsIgnoreCase(user)) {
				ifUserIsAMember = true;
				break;
			}
		}
		
		return ifUserIsAMember;
	}

	/**
	 * 
	 * @param groupID
	 * @return
	 */
	public Group getGroup(String groupID) {
		return dbHandler.getGroup(groupID);
	}

	/**
	 * 
	 * @param groupID
	 * @return
	 * @throws CustomException 
	 */
	public ArrayList<Message> getMessagesOfGroup(String groupID,String userEmail) throws CustomException {
		Group group = dbHandler.getGroup(groupID);
		if(userIsMember(userEmail, group))		
			return dbHandler.getGroup(groupID).getMessages();
		else
			throw new CustomException("User","Un-Authorized access");
	}

	public void removeTheUser(String groupID, String email, String exUser) throws CustomException{
		Group group = dbHandler.getGroup(groupID);
		if(!userIsMember(email, group))		
			throw new CustomException("User","The user is not a member of the group.");
		if (group.getOwner().equals(exUser))
			throw new CustomException("User","The owner cannot be removed from the group");
		if (!group.getOwner().equals(email)) {
			if (!exUser.equals(email))
				throw new CustomException("User","The user is not allowed to remove other users.");
		}
		
		ArrayList<Membership> members = group.getUsers();

		for (Membership member : members) {
			if (member.getEmail().equalsIgnoreCase(exUser)){
				members.remove(member);
				group.setUsers(members);
				dbHandler.updateGroup(group);
				break;
			}
		}		
	}
	/**
	 * 
	 * @param groupID
	 * @throws CustomException 
	 */
	public void removeGroup(String groupID, String userEmail) throws CustomException {
		Group group;
		try {
			group = dbHandler.getGroup(groupID);
		} catch (NoDocumentException nde) {
			throw new CustomException("user","The group specified does not exist.");
		}
		
		if(!group.getOwner().equalsIgnoreCase(userEmail))
			throw new CustomException("user","Unauthorized action, only the group "
					+ "owner can delete the group");
		dbHandler.deleteGroup(groupID);
	}

	public static void main(String[] args) throws Exception {
		GroupHandler gh = new GroupHandler("127.0.0.1");
		//gh.addNewGroup("Samax", "saman.bonab@gmail.com");
		//System.out.println(gh.addNewMessageToGroup("669caff1efad4a2bb2567a3682630758", new Message("cesarm@unimelb.edu.au", "hi everybody", 12346443)));
		//System.out.println(gh.confirmNewUser("669caff1efad4a2bb2567a3682630758", "samani", "o4kstpm4ec3cvc75lnhsui9g0fpa9tgo"));
		//System.out.println(gh.confirmNewUser("669caff1efad4a2bb2567a3682630758", "cesarm@unimelb.edu.au", "o4kstpm4ec3cvc75lnhsui9g0fpa9tgo"));
		//gh.addNewMessageToGroup("70c08dba008d40fca436dc3738dfe112", new Message("edwinsp@student.unimelb.edu.au", "hola", 1));

	}
}
