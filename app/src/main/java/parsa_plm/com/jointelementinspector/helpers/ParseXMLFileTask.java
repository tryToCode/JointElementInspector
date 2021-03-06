package parsa_plm.com.jointelementinspector.helpers;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import parsa_plm.com.jointelementinspector.activities.MainActivity;
import parsa_plm.com.jointelementinspector.activities.OpenFileActivity;
import parsa_plm.com.jointelementinspector.models.ExpandableListHeader;
import parsa_plm.com.jointelementinspector.models.ExpandableListItem;
import parsa_plm.com.jointelementinspector.models.Occurrence;
import parsa_plm.com.jointelementinspector.utils.AppConstants;
import parsa_plm.com.jointelementinspector.utils.Utility;

public class ParseXMLFileTask extends AsyncTask<File, Void, ExpandableListHeader> {
    private final String TAG = getClass().getName();
    private ProgressDialog mProgressDialog;
    private String mFilePath;
    private Context mContext;

    public ParseXMLFileTask(Context context, String filePath) {
        this.mFilePath = filePath;
        this.mContext = context;
    }
    protected void onPreExecute() {
        this.mProgressDialog = new ProgressDialog(this.mContext);
        this.mProgressDialog.setMessage(AppConstants.DATA_PROCESS);
        this.mProgressDialog.setCancelable(false);
        this.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.mProgressDialog.show();
    }
    // 20161214 need to expand later, method exacting for better maintain later
    // 20170113: two methods extracted, more readable code later
    protected ExpandableListHeader doInBackground(File... file) {
        // using Parcelable to send custom data
        // set default parameter to NotFound, if there is some thing wrong with xml parse
        // "Not Found" should be displayed to user
        DocumentBuilder domBuilder = null;
        ExpandableListHeader expandableListHeader = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                domBuilder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                Log.d("ParserException", e.toString());
            }
            File fileToParse = new File(this.mFilePath);
            // 20161125: pass file directory to fragment to load more data
            String fileDirectory = fileToParse.getAbsolutePath();
            String imagePath = fileDirectory.substring(0, fileDirectory.lastIndexOf('.'));
            Document dom = domBuilder.parse(fileToParse);
            Element plmXML = dom.getDocumentElement();
            NodeList occurrence = plmXML.getElementsByTagName("Occurrence");
            NodeList associatedAttachment = plmXML.getElementsByTagName("AssociatedAttachment");
            NodeList productRevision = plmXML.getElementsByTagName("ProductRevision");
            // 20160826: design revision node list
            NodeList designRevision = plmXML.getElementsByTagName("DesignRevision");
            NodeList form = plmXML.getElementsByTagName("Form");
            String associatedAttachmentRefs = null;
            String instancedRef = null;
            String occurrenceRefs = null;
            // 20160817: compute data without nested loop
            // 20170203: delete for loop
            Element firstOccu = (Element) occurrence.item(0);
            // prepare instanceRef to search product revision
            instancedRef = firstOccu.getAttribute("instancedRef");
            // obtain associated attachments to search associated attachment
            associatedAttachmentRefs = firstOccu.getAttribute("associatedAttachmentRefs");
            // obtain child occurrence to prepare expand list
            occurrenceRefs = firstOccu.getAttribute("occurrenceRefs");
            expandableListHeader = constructHeader(instancedRef, associatedAttachmentRefs, occurrenceRefs, imagePath, occurrence, associatedAttachment
                    , designRevision, productRevision, form);
        } catch (Exception e) {
            System.out.println(e);
        }
        return expandableListHeader;
    }
    protected void onPostExecute(ExpandableListHeader expandableListHeader) {
        if (expandableListHeader != null && this.mProgressDialog != null && this.mProgressDialog.isShowing()) {
            this.mProgressDialog.dismiss();
            Intent intent = new Intent(mContext, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(AppConstants.PARCELABLE, expandableListHeader);
            this.mContext.startActivity(intent);
        }
    }
    /*
        this method find all parameter to construct the expand list header.
        extract method getChildOccurrenceList which collect child occurrence with type weld point as list
     */
    private ExpandableListHeader constructHeader(String instancedRef, String associatedAttachmentRefs, String occurrenceRefs, String imagePath,
                                                 NodeList occurrence, NodeList associatedAttachment, NodeList designRevision, NodeList productRevision, NodeList form) {
        String partName = "NotFound";
        String partNr = "NotFound";
        String orderNr = "NotFound";
        String inspector = "NotFound";
        String inspectorDate = "NotFound";
        String vehicle = "NotFound";
        String inspectorTimeSpan = "NotFound";
        String frequency = "NotFound";
        String inspectorMethod = "NotFound";
        String inspectorScope = "NotFound";
        String inspectorNorm = "NotFound";
        // item type for header
        String type = "NotFound";
        String formRole = "IMAN_master_form";
        // get part name
        if (!notNullAndEmpty(instancedRef)) {
            Log.i(TAG, "constructHeader: " + "instancedRef is null or empty");
            return null;
        }
        String id4ProductRevision = instancedRef.substring(1);
        for (int l = 0; l < productRevision.getLength(); ++l) {
            Element eleProRev = (Element) productRevision.item(l);
            String idRevision = eleProRev.getAttribute("id");
            if (!notNullAndEmpty(id4ProductRevision)) {
                Log.i(TAG, "constructHeader: " + "id4ProductRevision is null or empty");
                return null;
            }
            if (id4ProductRevision.trim().equalsIgnoreCase(idRevision.trim())) {
                partName = eleProRev.getAttribute("name");
                type = eleProRev.getAttribute("subType");
                break;
            }
        }
        String[] associatedAttachmentIds = null;
        if (notNullAndEmpty(associatedAttachmentRefs))
            associatedAttachmentIds = associatedAttachmentRefs.split("#");
        String associatedAttachmentRole = null;
        String associateAttachmentId = null;
        String attachmentRef = null;
        String childRefs = null;
        if (associatedAttachmentIds.length == 0) {
            Log.i(TAG, "constructHeader: " + "associatedAttachmentIds is empty");
            return null;
        }
        IdLoop:
        for (String id : associatedAttachmentIds) {
            for (int k = 0; k < associatedAttachment.getLength(); ++k) {
                Element eleAsso = (Element) associatedAttachment.item(k);
                associatedAttachmentRole = eleAsso.getAttribute("role");
                associateAttachmentId = eleAsso.getAttribute("id");
                if (id.equalsIgnoreCase(associateAttachmentId) && associatedAttachmentRole.equalsIgnoreCase(formRole)) {
                    attachmentRef = eleAsso.getAttribute("attachmentRef");
                    // für weitere Attribute
                    childRefs = eleAsso.getAttribute("childRefs");
                    break IdLoop;
                }
            }
        }
        if (notNullAndEmpty(attachmentRef))
            attachmentRef = attachmentRef.substring(1);
        if (!notNullAndEmpty(attachmentRef)) {
            Log.i(TAG, "constructHeader: " + "attachmentRef is null or empty");
            return null;
        }
        formLoop:
        for (int k = 0; k < form.getLength(); ++k) {
            Element eleForm = (Element) form.item(k);
            if (attachmentRef.trim().equalsIgnoreCase(eleForm.getAttribute("id").trim())) {
                partNr = eleForm.getAttribute("name");
                NodeList nodeOfForm = eleForm.getElementsByTagName("UserValue");
                for (int i = 0; i < nodeOfForm.getLength(); i++) {
                    Element eleNode = (Element) nodeOfForm.item(i);
                    String nodeTitle = eleNode.getAttribute("title");
                    switch (nodeTitle) {
                        case "a2_InspDate":
                            inspectorDate = eleNode.getAttribute("value");
                            break;
                        case "a2_Inspector":
                            inspector = eleNode.getAttribute("value");
                            break;
                        case "a2_OrderNr":
                            orderNr = eleNode.getAttribute("value");
                            break;
                        case "project_id":
                            vehicle = eleNode.getAttribute("value");
                            break;
                    }
                }
                break formLoop;
            }
        }
        // obtain attribute time span and frequency
        String idOfForm4MoreAttri = null;
        if (notNullAndEmpty(childRefs))
            idOfForm4MoreAttri = findId4InspeAttri(childRefs, associatedAttachment);
        // use id of form to find more attribute
        if (!notNullAndEmpty(idOfForm4MoreAttri)) {
            Log.i(TAG, "constructHeader: " + "idOfForm4MoreAttri is null or empty");
            return null;
        }
        for (int k = 0; k < form.getLength(); ++k) {
            Element eleForm = (Element) form.item(k);
            // better to use TRIM() for sure
            if (idOfForm4MoreAttri.trim().equalsIgnoreCase(eleForm.getAttribute("id").trim())) {
                NodeList nodeOfForm = eleForm.getElementsByTagName("UserValue");
                for (int i = 0; i < nodeOfForm.getLength(); i++) {
                    Element eleNode = (Element) nodeOfForm.item(i);
                    String nodeTitle = eleNode.getAttribute("title");
                    switch (nodeTitle) {
                        case "a2_InspCycle":
                            frequency = eleNode.getAttribute("value");
                            break;
                        case "a2_InspMethod":
                            inspectorMethod = eleNode.getAttribute("value");
                            break;
                        case "a2_InspScope":
                            inspectorScope = eleNode.getAttribute("value");
                            break;
                        case "a2_InspectionTimespan":
                            inspectorTimeSpan = eleNode.getAttribute("value");
                            break;
                        case "a2_InspRegNORM":
                            inspectorNorm = eleNode.getAttribute("value");
                            break;
                    }
                }
            }
        }
        List<ExpandableListItem> childOfOccurrence = getChildOccurrenceList(occurrenceRefs, formRole, occurrence, associatedAttachment,
                productRevision, designRevision, form);
        return new ExpandableListHeader.Builder()
                .setPartName(partName)
                .setPartNr(partNr)
                .setOrderNr(orderNr)
                .setInspector(inspector)
                .setInspectorDate(inspectorDate)
                .setVehicle(vehicle)
                .setInspectorTimeSpan(inspectorTimeSpan)
                .setFrequency(frequency)
                .setType(type)
                .setInspectorMethod(inspectorMethod)
                .setInspectorScope(inspectorScope)
                .setInspectorNorm(inspectorNorm)
                .setFileDirectory(imagePath)
                .setChildOfOccurrenceList(childOfOccurrence)
                .build();
    }
    private String findId4InspeAttri(String childRefs, NodeList associatedAttachment) {
        String idOfForm4InspeAttri = null;
        // first spilt string is empty
        // idFindAttachment contains blank!!!!!, use TRIM() for sure
        String id2FindAttachment = childRefs.split("#")[1];
        for (int k = 0; k < associatedAttachment.getLength(); ++k) {
            Element eleAssociated = (Element) associatedAttachment.item(k);
            // idCompareTo contains blank
            String idCompareTo = eleAssociated.getAttribute("id");
            if (!notNullAndEmpty(idCompareTo)) {
                Log.i(TAG, "constructHeader: " + "idCompareTo is null or empty");
                return null;
            }
            if (idCompareTo.trim().equalsIgnoreCase(id2FindAttachment.trim())) {
                idOfForm4InspeAttri = eleAssociated.getAttribute("attachmentRef");
                if (!notNullAndEmpty(idOfForm4InspeAttri)) {
                    Log.i(TAG, "constructHeader: " + "idOfForm4InspeAttri is null or empty");
                    return null;
                }
                idOfForm4InspeAttri = idOfForm4InspeAttri.substring(1);
                break;
            }
        }
        return idOfForm4InspeAttri;
    }
    private List<ExpandableListItem> getChildOccurrenceList(String occurrenceRefs, String formRole, NodeList occurrence, NodeList associatedAttachment,
                                                            NodeList productRevision, NodeList designRevision, NodeList form) {
        List<ExpandableListItem> childOfOccurrence = new ArrayList<>();
        // 20160826: move on with child occurrence
        String[] idsOfChildOccu = null;
        if (notNullAndEmpty(occurrenceRefs)) {
            idsOfChildOccu = occurrenceRefs.split(" ");
        }
        // find child occurrence and create ExpandableListItem
        // 20160829: child of child occurrence found and create Occurrence
        for (String id : idsOfChildOccu) {
            // local variable ExpandlistItem, recreate for every loop
            ExpandableListItem item = null;
            // local variable associated AttachmentRefs
            String childAssociatedAttachmentRefs = null;
            // local variable instanceRef
            String childInstancedRef = null;
            // ids of weld points of the child occurrence
            String idsOfItemWeldPoint = null;
            // item name for expandableListItem
            String itemName = null;
            // item type
            String itemType = null;
            // item Nr
            String itemNr = null;
            // child of child occurrence, could be null
            List<Occurrence> itemOfChild = null;
            for (int k = 0; k < occurrence.getLength(); ++k) {
                Element eleChildOcc = (Element) occurrence.item(k);
                if (id.trim().equalsIgnoreCase(eleChildOcc.getAttribute("id").trim())) {
                    childAssociatedAttachmentRefs = eleChildOcc.getAttribute("associatedAttachmentRefs");
                    childInstancedRef = eleChildOcc.getAttribute("instancedRef");
                    idsOfItemWeldPoint = eleChildOcc.getAttribute("occurrenceRefs");
                }
            }
            // first find item name and item type, if id not in design revision, muss be in product revision
            if (!notNullAndEmpty(childInstancedRef)) {
                Log.i(TAG, "constructHeader: " + "childInstancedRef is null or empty");
                return null;
            }
            String idOfRevisions = childInstancedRef.split("#")[1];
            boolean itemFound = false;
            for (int k = 0; k < designRevision.getLength(); ++k) {
                Element eleDesignRevison = (Element) designRevision.item(k);
                if (idOfRevisions.trim().equalsIgnoreCase(eleDesignRevison.getAttribute("id").trim())) {
                    itemName = eleDesignRevison.getAttribute("name");
                    itemType = eleDesignRevison.getAttribute("subType");
                    if (itemName != null && itemType != null)
                        itemFound = true;
                }
            }
            if (!itemFound) {
                for (int k = 0; k < productRevision.getLength(); ++k) {
                    Element eleProductRevision = (Element) productRevision.item(k);
                    if (idOfRevisions.trim().equalsIgnoreCase(eleProductRevision.getAttribute("id").trim())) {
                        itemName = eleProductRevision.getAttribute("name");
                        itemType = eleProductRevision.getAttribute("subType");
                    }
                }
            }
            // now use associated attachment to find item number
            if (!notNullAndEmpty(childAssociatedAttachmentRefs)) {
                Log.i(TAG, "constructHeader: " + "childAssociatedAttachmentRefs is null or empty");
                return null;
            }
            String[] idsOfChildAttachment = childAssociatedAttachmentRefs.split("#");
            String idOfForm4ItemNr = null;
            childAssocitedLoop:
            for (String idOfChild : idsOfChildAttachment) {
                for (int k = 0; k < associatedAttachment.getLength(); ++k) {
                    Element eleChildAssociated = (Element) associatedAttachment.item(k);
                    if (idOfChild.trim().equalsIgnoreCase(eleChildAssociated.getAttribute("id").trim())
                            && formRole.equalsIgnoreCase(eleChildAssociated.getAttribute("role"))) {
                        idOfForm4ItemNr = eleChildAssociated.getAttribute("attachmentRef");
                        break childAssocitedLoop;
                    }
                }
            }
            // now use forms id to find form and then get item number
            if (!notNullAndEmpty(idOfForm4ItemNr)) {
                Log.i(TAG, "constructHeader: " + "idOfForm4ItemNr is null or empty");
                return null;
            }
            String idSplitted = idOfForm4ItemNr.split("#")[1];
            for (int k = 0; k < form.getLength(); k++) {
                Element eleChildForm = (Element) form.item(k);
                if (idSplitted.trim().equalsIgnoreCase(eleChildForm.getAttribute("id").trim())) {
                    itemNr = eleChildForm.getAttribute("name");
                }
            }
            // 20170203: extract method for more readable
            itemOfChild = getChildWeldPoints(occurrence, associatedAttachment, form, idsOfItemWeldPoint, itemOfChild);
            // now time to create ExpandablelistItem
            if (itemName != null && itemNr != null && itemType != null) {
                item = new ExpandableListItem(itemName, itemNr, itemType, itemOfChild != null ? itemOfChild : null);
                childOfOccurrence.add(item);
            }
        }
        return childOfOccurrence;
    }
    private List<Occurrence> getChildWeldPoints(NodeList occurrence, NodeList associatedAttachment, NodeList form, String idsOfItemWeldPoint, List<Occurrence> itemOfChild) {
        // last use idsItemWeldPoint to find weld point
        // first version, obtain weld point name to display list structure, late more attribute
        if (!notNullAndEmpty(idsOfItemWeldPoint)) {
            Log.i(TAG, "constructHeader: " + "idsOfItemWeldPoint is null or empty");
            return null;
        }
        itemOfChild = new ArrayList<>();
        String[] ids = idsOfItemWeldPoint.split(" ");
        for (String id4WeldPoint : ids) {
            if (!notNullAndEmpty(id4WeldPoint)) {
                Log.i(TAG, "constructHeader: " + "id4WeldPoint is null or empty");
                return null;
            }
            String name = null;
            String joints_itemType = null;
            Occurrence weldPoint = null;
            // 20161021: associated attachments to find characters
            String associatedARs = null;
            // 20170510: weld points transform matrix
            String transformMatrix = null;
            occuLoop:
            for (int k = 0; k < occurrence.getLength(); k++) {
                Element eleOccu = (Element) occurrence.item(k);
                if (id4WeldPoint.trim().equalsIgnoreCase(eleOccu.getAttribute("id").trim())) {
                    associatedARs = eleOccu.getAttribute("associatedAttachmentRefs");
                    NodeList nodes = eleOccu.getElementsByTagName("UserValue");
                    NodeList transform = eleOccu.getElementsByTagName("Transform");
                    Node matrixChildren = transform.item(0).getFirstChild();
                    transformMatrix = matrixChildren.getNodeValue();
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element eleNode = (Element) nodes.item(i);
                        String nodeTitle = eleNode.getAttribute("title");
                        if (nodeTitle.equalsIgnoreCase("OccurrenceName")) {
                            name = eleNode.getAttribute("value");
                            break occuLoop;
                        }
                    }
                }
            }
            String id4Form = null;
            // check associated attachments
            if (!notNullAndEmpty(associatedARs)) {
                Log.i(TAG, "constructHeader: " + "associatedARs is null or empty");
                return null;
            }
            String[] aRs = associatedARs.split("#");
            attachmentsLoop:
            for (String id4Character : aRs) {
                if (notNullAndEmpty(id4Character))
                    return null;
                for (int k = 0; k < associatedAttachment.getLength(); ++k) {
                    Element eleAss = (Element) associatedAttachment.item(k);
                    if (id4Character.trim().equalsIgnoreCase(eleAss.getAttribute("id").trim())
                            && "TC_Feature_Form_Relation".equalsIgnoreCase(eleAss.getAttribute("role"))) {
                        id4Form = eleAss.getAttribute("attachmentRef");
                        break attachmentsLoop;
                    }
                }
            }
            // 20161021: map which hold all key value paar for wild points
            Map<String, String> weldPointsAttrs = new HashMap<>();
            // 20170203: extract method
            joints_itemType = getWeldPointAttribute(form, id4Form, weldPointsAttrs);
            if (!notNullAndEmpty(name) || !notNullAndEmpty(joints_itemType))
                return null;
            String joins_it = joints_itemType.split(" ")[0];
            if (!notNullAndEmpty(transformMatrix)) {
                Log.i(TAG, "constructHeader: " + "transformMatrix is null or empty");
                return null;
            }
            double[][] matrix = Utility.generateMatrix(transformMatrix);
            weldPoint = new Occurrence(name, joins_it.trim(),
                    Utility.scalePosition(matrix[0][3]),
                    Utility.scalePosition(matrix[1][3]),
                    Utility.scalePosition(matrix[2][3]),weldPointsAttrs);
            itemOfChild.add(weldPoint);
        }
        return itemOfChild;
    }
    private String getWeldPointAttribute(NodeList form, String id4Form, Map<String, String> character) {
        String joints_itemType = null;
        if (!notNullAndEmpty(id4Form)) {
            Log.i(TAG, "constructHeader: " + "id4Form is null or empty");
            return null;
        }
        String id4FormSplitted = id4Form.split("#")[1];
        for (int k = 0; k < form.getLength(); ++k) {
            Element eleForm = (Element) form.item(k);
            if (id4FormSplitted.trim().equalsIgnoreCase(eleForm.getAttribute("id").trim())) {
                joints_itemType = eleForm.getAttribute("subType");
                NodeList nodes = eleForm.getElementsByTagName("UserValue");
                // 20170217: get attribute and store in list
                for (int i = 0; i < nodes.getLength(); ++i) {
                    Element eleNode = (Element) nodes.item(i);
                    String nodeTitle = eleNode.getAttribute("title");
                    getWPAttribute(character, eleNode, nodeTitle);
                }
                // 20170217: now wir should check if attribute has default value
                setDefaultValue(nodes, character);
            }
        }
        return joints_itemType;
    }
    private void setDefaultValue(NodeList nodes, Map<String, String> character) {
        for (String key : character.keySet()) {
            for (int i = 0; i < nodes.getLength(); ++i) {
                Element eleNode = (Element) nodes.item(i);
                String nodeTitle = eleNode.getAttribute("title");
                String defaultValueTitle = key + "_Soll";
                if (nodeTitle.trim().equalsIgnoreCase(defaultValueTitle)) {
                    String nodeValue = eleNode.getAttribute("value");
                    character.put(key, nodeValue);
                }
            }
        }
    }
    private void getWPAttribute(Map<String, String> character, Element eleNode, String nodeTitle) {
        String nodeValue;
        switch (nodeTitle) {
            case "a2_100_Crack":
                nodeValue = eleNode.getAttribute("value");
                character.put("Crack", nodeValue);
                break;
            case "a2_104_CraterCrack":
                nodeValue = eleNode.getAttribute("value");
                character.put("CraterCrack", nodeValue);
                break;
            case "a2_2017_SurfacePore":
                nodeValue = eleNode.getAttribute("value");
                character.put("SurfacePore", nodeValue);
                break;
            case "a2_2025_EndCraterPipe":
                nodeValue = eleNode.getAttribute("value");
                character.put("EndCraterPipe", nodeValue);
                break;
            case "a2_401_LackOfFusion":
                nodeValue = eleNode.getAttribute("value");
                character.put("LackOfFusion", nodeValue);
                break;
            case "a2_4021_IncRootPenetration":
                nodeValue = eleNode.getAttribute("value");
                character.put("IncRootPenetration", nodeValue);
                break;
            case "a2_5011_ContinousUndercut":
                nodeValue = eleNode.getAttribute("value");
                character.put("ContinousUndercut", nodeValue);
                break;
            case "a2_5012_IntUndercut":
                nodeValue = eleNode.getAttribute("value");
                character.put("IntUndercut", nodeValue);
                break;
            case "a2_5013_ShrinkGrooves":
                nodeValue = eleNode.getAttribute("value");
                character.put("ShrinkGrooves", nodeValue);
                break;
            case "a2_502_ExcWeldMetal":
                nodeValue = eleNode.getAttribute("value");
                character.put("ExcWeldMetal", nodeValue);
                break;
            case "a2_503_ExcConvex":
                nodeValue = eleNode.getAttribute("value");
                character.put("ExcConvex", nodeValue);
                break;
            case "a2_504_ExcPenetration":
                nodeValue = eleNode.getAttribute("value");
                character.put("ExcPenetration", nodeValue);
                break;
            case "a2_505_IncWeldToe":
                nodeValue = eleNode.getAttribute("value");
                character.put("IncWeldToe", nodeValue);
                break;
            case "a2_506_Overlap":
                nodeValue = eleNode.getAttribute("value");
                character.put("Overlap", nodeValue);
                break;
            case "a2_509_Sagging":
                nodeValue = eleNode.getAttribute("value");
                character.put("Sagging", nodeValue);
                break;
            case "a2_510_BurnThrough":
                nodeValue = eleNode.getAttribute("value");
                character.put("BurnThrough", nodeValue);
                break;
            case "a2_511_IncFilledGroove":
                nodeValue = eleNode.getAttribute("value");
                character.put("IncFilledGroove", nodeValue);
                break;
            case "a2_512_ExcAsymFilledWeld":
                nodeValue = eleNode.getAttribute("value");
                character.put("ExcAsymFilledWeld", nodeValue);
                break;
            case "a2_515_RootConcavity":
                nodeValue = eleNode.getAttribute("value");
                character.put("RootConcavity", nodeValue);
                break;
            case "a2_516_RootPorosity":
                nodeValue = eleNode.getAttribute("value");
                character.put("RootPorosity", nodeValue);
                break;
            case "a2_517_PoorRestart":
                nodeValue = eleNode.getAttribute("value");
                character.put("PoorRestart", nodeValue);
                break;
            case "a2_5213_InsThroatThick":
                nodeValue = eleNode.getAttribute("value");
                character.put("InsThroatThick", nodeValue);
                break;
            case "a2_5214_ExcThoratThick":
                nodeValue = eleNode.getAttribute("value");
                character.put("ExcThoratThick", nodeValue);
                break;
            case "a2_601_ArcStrike":
                nodeValue = eleNode.getAttribute("value");
                character.put("ArcStrike", nodeValue);
                break;
            case "a2_602_Spatter":
                nodeValue = eleNode.getAttribute("value");
                character.put("Spatter", nodeValue);
                break;
            case "a2_610_TempColours":
                nodeValue = eleNode.getAttribute("value");
                character.put("TempColours", nodeValue);
                break;
        }
    }
    private boolean notNullAndEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
