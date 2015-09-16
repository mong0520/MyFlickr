/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package myflickr.core;

import com.flickr4java.flickr.groups.Group;

/**
 *
 * @author neil
 */
public class GroupWrapper {
    private Group group;
    public GroupWrapper(Group g){
        this.group = g;
    }

    /**
     * @return the group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(Group group) {
        this.group = group;
    }

}
