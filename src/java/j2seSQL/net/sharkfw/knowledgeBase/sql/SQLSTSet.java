package net.sharkfw.knowledgeBase.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import net.sharkfw.knowledgeBase.AbstractSTSet;
import net.sharkfw.knowledgeBase.FragmentationParameter;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SemanticTag;
import net.sharkfw.knowledgeBase.SharkCSAlgebra;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.system.Iterator2Enumeration;

/**
 *
 * @author thsc
 */
public class SQLSTSet extends AbstractSTSet implements STSet {
    private final SQLSharkKB kb;

    SQLSTSet(SQLSharkKB kb) {
        this.kb = kb;
    }
    
    SQLSharkKB getSSQLSharkKB() {
        return this.kb;
    }
    
    /**
     * Creates a new entry in tags table.
     * @param kb
     * @param name
     * @param ewkt
     * @param startTime
     * @param durationTime
     * @param hidden
     * @param type use SQLSharkKB.SemanticTag_Type etc.
     * @param sis
     * @return
     * @throws SharkKBException
     */
    protected SQLSemanticTagStorage createSQLSemanticTag(
            SQLSharkKB kb,
            String name, 
            String ewkt, // if spatial semantic tag
            long startTime, // if time semantic tag
            long durationTime, // if time semantic tag
            boolean hidden, 
            int type,
            String[] sis) throws SharkKBException {
        
        return new SQLSemanticTagStorage(kb, name, ewkt, startTime, durationTime, 
            hidden, type, sis);
    }
    
    /**
     *
     * @param si
     * @return
     * @throws SharkKBException
     */
    protected SQLSemanticTagStorage getSQLSemanticTagStorage(String si) throws SharkKBException {    
        return this.getSQLSemanticTagStorage(new String[]{si});
    }
    
    protected SQLSemanticTagStorage getSQLSemanticTagStorage(String[] sis) throws SharkKBException {
        if(sis == null || sis.length == 0) {
            return null;
        }
        
        Statement statement = null;
        
        try {
            statement  = this.kb.getConnection().createStatement();
            
            String sqlString = "select * from " + SQLSharkKB.ST_TABLE + 
                    " where id = (select stid from " + 
                    SQLSharkKB.SI_TABLE + " where si = '" + sis[0];
            
            for(int i = 1; i < sis.length; i++) {
                sqlString += " OR si = '" + sis[i] + "'";
            }
            
            sqlString += "');";
            
            ResultSet result = statement.executeQuery(sqlString);
            
            if(!result.next()) {
                // nothing found - leave
                return null;
            }
            
            int stID = result.getInt("id");
            
            return new SQLSemanticTagStorage(kb, stID);
            
        }
        catch(SQLException e) {
            throw new SharkKBException(e.getLocalizedMessage());
        }
        finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch (SQLException ex) {
                    // ignore
                }
            }
        }
    }
    
    /**
     * Creates iterator of tags of given type. Note: Semantic Tags cover also
     * time, spatial and peer semantic tags
     * @param type
     * @return
     * @throws SharkKBException 
     */
    protected Iterator tags(int type) throws SharkKBException {
        List<SemanticTag> tagList = new ArrayList<>();
        Statement statement = null;

        try {
            statement  = this.kb.getConnection().createStatement();
            
            String sqlString = "select id from " + SQLSharkKB.ST_TABLE;
            
            // select specific tag type if not of general semantic tag type
            switch(type) {
                case SQLSharkKB.PEER_SEMANTIC_TAG_TYPE:
                case SQLSharkKB.SPATIAL_SEMANTIC_TAG_TYPE:
                case SQLSharkKB.TIME_SEMANTIC_TAG_TYPE:
                    sqlString += " where st_type = " + type;
                    break;
            }
            
            ResultSet result = statement.executeQuery(sqlString);
            
            while(result.next()) {
                // something found
                int id = result.getInt("id");
                SQLSemanticTagStorage sqlST = new SQLSemanticTagStorage(this.kb, id);
                
                // wrap into tag of correct type:
                SemanticTag newTag;
                switch(type) {
                    case SQLSharkKB.PEER_SEMANTIC_TAG_TYPE:
                        newTag = new SQLPeerSemanticTag(this.kb, sqlST);
                        break;
                        
                    case SQLSharkKB.SPATIAL_SEMANTIC_TAG_TYPE:
                        newTag = new SQLSpatialSemanticTag(this.kb, sqlST);
                        break;
                        
                    case SQLSharkKB.TIME_SEMANTIC_TAG_TYPE:
                        newTag = new SQLTimeSemanticTag(this.kb, sqlST);
                        break;
                        
                    default:
                        newTag = new SQLSemanticTag(this.kb, sqlST);
                }
                
                // add to list
                tagList.add(newTag);
            }
        }
        catch(SQLException e) {
            throw new SharkKBException(e.getLocalizedMessage());
        }
        finally {
            if(statement != null) {
                try {
                    statement.close();
                } catch (SQLException ex) {
                    // ignore
                }
            }
        }
        
        return tagList.iterator();
    }
    
    @Override
    public SemanticTag merge(SemanticTag tag) throws SharkKBException {
        return SharkCSAlgebra.merge(this, tag);
    }

    @Override
    public SemanticTag createSemanticTag(String name, String[] sis) throws SharkKBException {
        SQLSemanticTagStorage sqlSTStorage = this.createSQLSemanticTag(kb, name, null, 0, 0, false, SQLSharkKB.SEMANTIC_TAG_TYPE, sis);
        
        return new SQLSemanticTag(this.kb, sqlSTStorage);
    }

    @Override
    public SemanticTag createSemanticTag(String name, String si) throws SharkKBException {
        return this.createSemanticTag(name, new String[]{si});
    }

    @Override
    public void removeSemanticTag(SemanticTag tag) throws SharkKBException {
        String si = null;
        if(tag.getSI() != null && tag.getSI().length > 0) {
            si = tag.getSI()[0];
        }
        
        SQLSemanticTagStorage sqlST = this.getSQLSemanticTagStorage(si);
        sqlST.remove();
    }

    @Override
    public void setEnumerateHiddenTags(boolean hide) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Enumeration<SemanticTag> tags() throws SharkKBException {
        return new Iterator2Enumeration(this.stTags());
    }

    @Override
    public SemanticTag getSemanticTag(String[] si) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SemanticTag getSemanticTag(String si) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<SemanticTag> getSemanticTagByName(String pattern) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public STSet fragment(SemanticTag anchor) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public STSet fragment(SemanticTag anchor, FragmentationParameter fp) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public STSet contextualize(Enumeration<SemanticTag> anchorSet, FragmentationParameter fp) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public STSet contextualize(Enumeration<SemanticTag> anchorSet) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public STSet contextualize(STSet context, FragmentationParameter fp) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public STSet contextualize(STSet context) throws SharkKBException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<SemanticTag> stTags() throws SharkKBException {
        return this.tags(SQLSharkKB.SEMANTIC_TAG_TYPE);
    }
}