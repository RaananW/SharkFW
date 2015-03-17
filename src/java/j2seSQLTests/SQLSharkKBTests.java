import net.sharkfw.knowledgeBase.SNSemanticTag;
import net.sharkfw.knowledgeBase.STSet;
import net.sharkfw.knowledgeBase.SemanticNet;
import net.sharkfw.knowledgeBase.SemanticTag;
import net.sharkfw.knowledgeBase.SharkCSAlgebra;
import net.sharkfw.knowledgeBase.SharkKBException;
import net.sharkfw.knowledgeBase.TXSemanticTag;
import net.sharkfw.knowledgeBase.Taxonomy;
import net.sharkfw.knowledgeBase.TimeSTSet;
import net.sharkfw.knowledgeBase.TimeSemanticTag;
import net.sharkfw.knowledgeBase.sql.SQLSharkKB;
import net.sharkfw.system.L;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author thsc
 */
public class SQLSharkKBTests {
    
    public SQLSharkKBTests() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

     @Test
     public void createDB() throws SharkKBException {
         L.setLogLevel(L.LOGLEVEL_ALL);
         SQLSharkKB kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
         
         // I can close 
         kb.close();
         
         // and reconnect
         kb.reconnect();
         
         // and close again
         kb.close();
     }
     
     @Test
     public void basicSTSetTests() throws SharkKBException {
        L.setLogLevel(L.LOGLEVEL_ALL);
        SQLSharkKB kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        kb.drop();
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        
        TimeSTSet timeSTSet = kb.getTimeSTSet();
        
        TimeSemanticTag tst = timeSTSet.createTimeSemanticTag(TimeSemanticTag.FIRST_MILLISECOND_EVER, TimeSemanticTag.FOREVER);
        
        tst.setProperty("p1", "v1");
        
        String property = tst.getProperty("p1");
        
        Assert.assertEquals(property, "v1");
        
        // test persistency
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        timeSTSet = kb.getTimeSTSet();
        tst = timeSTSet.timeTags().nextElement();
        property = tst.getProperty("p1");
        Assert.assertEquals(property, "v1");
     }
     
     @Test
     public void addAndRemoveOnSemanticTag() throws SharkKBException {
        L.setLogLevel(L.LOGLEVEL_ALL);
        SQLSharkKB kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        kb.drop();
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        
        STSet stSet = kb.getTopicSTSet();
        SemanticTag st = stSet.createSemanticTag("Shark", "http://www.sharksystem.net");
        
        st.setProperty("p1", "v1");
        String property = st.getProperty("p1");
        Assert.assertEquals(property, "v1");
        
        st.setName("SharkFW");
        Assert.assertEquals("SharkFW", st.getName());
        
        st.addSI("http://www.sharkfw.net");
        st.removeSI("http://www.sharksystem.net");
        
        // test persistency
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        
        
        stSet = kb.getTopicSTSet();
        st = stSet.getSemanticTag("http://www.sharksystem.net");
        Assert.assertNull(st);
        
        st = stSet.getSemanticTag("http://www.sharkfw.net");
        Assert.assertNotNull(st);
        
        Assert.assertEquals("SharkFW", st.getName());
        
        property = st.getProperty("p1");
        Assert.assertEquals(property, "v1");
     }
     
     @Test
     public void semanticNet() throws SharkKBException {
        L.setLogLevel(L.LOGLEVEL_ALL);
        SQLSharkKB kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        kb.drop();
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        
        SemanticNet sn = kb.getTopicsAsSemanticNet();
        
        SNSemanticTag stA = sn.createSemanticTag("A", "http://a.de");
        SNSemanticTag stB = sn.createSemanticTag("B", "http://b.de");
        
        stA.setPredicate("p1", stB);
        
        SNSemanticTag stAA = sn.getSemanticTag("http://a.de");
        
        Assert.assertNotNull(stAA);
        
        SNSemanticTag stBB = stAA.targetTags("p1").nextElement();
        
        Assert.assertTrue(SharkCSAlgebra.identical(stB, stBB));
        
        // check persistency
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        
        sn = kb.getTopicsAsSemanticNet();
        
        stA = sn.getSemanticTag("http://a.de");
        stB = sn.getSemanticTag("http://b.de");
        
        Assert.assertNotNull(stA);
        
        stBB = stA.targetTags("p1").nextElement();
        
        Assert.assertTrue(SharkCSAlgebra.identical(stB, stBB));
     }
     
     @Test
     public void txTests() throws SharkKBException {
        L.setLogLevel(L.LOGLEVEL_ALL);
        SQLSharkKB kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        kb.drop();
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        
        Taxonomy tx = kb.getTopicsAsTaxonomy();
        
        TXSemanticTag txA = tx.createTXSemanticTag("A", "http://a.de");
        TXSemanticTag txB = tx.createTXSemanticTag("B", "http://b.de");
        
        txA.move(txB);
        
        TXSemanticTag txAA = tx.getSemanticTag("http://a.de");
        
        Assert.assertNotNull(txAA);
        
        TXSemanticTag txBB = txAA.getSuperTag();
        
        Assert.assertTrue(SharkCSAlgebra.identical(txB, txBB));

        // check persistency
        kb.close();
        kb = new SQLSharkKB("jdbc:postgresql://localhost:5432/SharkKB", "test", "test");
        
        tx = kb.getTopicsAsTaxonomy();
        
        txA = tx.getSemanticTag("http://a.de");
        txB = tx.getSemanticTag("http://b.de");
        
        Assert.assertNotNull(txA);
        Assert.assertNotNull(txB);
        
        txBB = txA.getSuperTag();
        
        Assert.assertTrue(SharkCSAlgebra.identical(txB, txBB));
        
        txAA = txB.getSubTags().nextElement();
        
        Assert.assertTrue(SharkCSAlgebra.identical(txA, txAA));
     }
}
