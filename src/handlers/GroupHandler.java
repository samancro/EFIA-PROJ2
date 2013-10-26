package handlers;

import java.util.ArrayList;

import data.Group;
import data.Membership;
import data.Message;
import exceptions.CustomException;


public class GroupHandler {
	DBHandler dbHandler;
	
	public GroupHandler(){
		dbHandler = new DBHandler();
	}
	
	/**
	 * @param groupName
	 * @param owner
	 * @throw CustomException In case the group name is repeated for that owner
	 */
	public void addNewGroup(String groupName, String owner) 
			throws CustomException {
		Group group = new Group(groupName, owner);
		ArrayList<Group> groups = dbHandler.getGroups(owner);
		if (groups.contains(group)) {
			throw new CustomException(CustomException.GROUP_NAME_UNAVAILABLE);
		}
		dbHandler.addNewGroup(group);		
	}
	
	//TODO Implement the invitation token logic
	/**
	 * 
	 * @param groupID
	 * @param newUserEmail
	 */
	public void addNewUserToGroup(String groupID, String newUserEmail) {
		Group group = dbHandler.getGroup(groupID);
		group.getUsers().add(new Membership(newUserEmail, ""));
		dbHandler.updateGroup(group);
	}
	
	/**
	 * 
	 * @param groupID
	 * @param newMessage
	 */
	public void addNewMessageToGroup(String groupID, Message newMessage){
		Group group = dbHandler.getGroup(groupID);
		group.getMessages().add(newMessage);
		dbHandler.updateGroup(group);
	}
	
	/**
	 * 
	 * @param groupID
	 * @return
	 */
	public Group getGroup(String groupID){
		return dbHandler.getGroup(groupID);
	}
	
	/**
	 * 
	 * @param groupID
	 * @return
	 */
	public ArrayList<Message> getMessagesOfGroup(String groupID){
		return dbHandler.getGroup(groupID).getMessages();
	}
	
	/**
	 * 
	 * @param groupID
	 */
	public void removeGroup(String groupID){
		dbHandler.deleteGroup(groupID);
	}
	
	public static void main(String[] args) {
		GroupHandler gh = new GroupHandler();
		//gh.addNewGroup("group2", "cesarm@student.unimelb.edu.au");
		//gh.addNewUserToGroup("70c08dba008d40fca436dc3738dfe112", "samana@unimelb.edu.au");
		//gh.addNewMessageToGroup("70c08dba008d40fca436dc3738dfe112", new Message("samana@student.unimelb.edu.au", "Hola cesar!", 1382706613626l));
		DBHandler dbh = gh.dbHandler;
		System.out.println(dbh.getGroups("samana@student.unimelb.edu.au"));
	}

}
