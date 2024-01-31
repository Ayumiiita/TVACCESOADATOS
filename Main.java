import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.sql.*;


public class Main {
    public static void main(String[] args) {

                try {
                    // Descargar y leer el archivo XML
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new File("C:\\Users\\Usuario\\Documents\\IdeaProjects\\TrabajoVoluntarioAccesos\\src\\adjus.xml"));
                    factory.setValidating(true);
                    System.out.println("Documento cargado y leído con éxito");

                    // Establecer la conexión a la base de datos Oracle
                    Class.forName("oracle.jdbc.driver.OracleDriver");
                    Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521/xe", "system", "system");
                    System.out.println("Conectado a la base de datos con éxito");
                    Statement statement = conn.createStatement();

                    // Crear la tabla para almacenar los datos
                    String query = "CREATE TABLE CONTRATOS (NIF VARCHAR2(255), ADJUDICATARIO VARCHAR2(255), OBJETO_GENERICO VARCHAR2(255), OBJETO VARCHAR2(255), " +
                            "FECHA_ADJUDICACION VARCHAR2(255), IMPORTE VARCHAR2(255), PROVEEDORES VARCHAR2(255), TIPO VARCHAR2(255))";

                    statement.execute(query);
                    System.out.println("Tabla contratos creada con éxito");


                    NodeList contratoNodes = doc.getElementsByTagName("Row");
                    String insert = "INSERT INTO CONTRATOS (NIF, ADJUDICATARIO, OBJETO_GENERICO, OBJETO, FECHA_ADJUDICACION, " +
                                    "IMPORTE, PROVEEDORES, TIPO) VALUES ( ? , ? , ? , ? , ? , ? , ?, ? )";

                    PreparedStatement insertStatement = conn.prepareStatement(insert);
                    //Obtener datos del archivo XML y almacenarlos en la tabla creada.
                    for (int i = 0; i < contratoNodes.getLength(); i++) {
                        try {
                        Element contrato = (Element) contratoNodes.item(i);
                        String nif = contrato.getElementsByTagName("Data").item(0).getTextContent();
                        String adjudicatario = contrato.getElementsByTagName("Data").item(1).getTextContent();
                        String objetogenerico = contrato.getElementsByTagName("Data").item(2).getTextContent();
                        String objeto = contrato.getElementsByTagName("Data").item(3).getTextContent();
                        String fecha = contrato.getElementsByTagName("Data").item(4).getTextContent();
                        String importe = contrato.getElementsByTagName("Data").item(5).getTextContent();
                        String proveedores = contrato.getElementsByTagName("Data").item(6).getTextContent();
                        String tipoContrato = contrato.getElementsByTagName("Data").item(7).getTextContent();


                        insertStatement.setString(1, nif);
                        insertStatement.setString(2, adjudicatario);
                        insertStatement.setString(3, objetogenerico);
                        insertStatement.setString(4, objeto);
                        insertStatement.setString(5, fecha);
                        insertStatement.setString(6, importe);
                        insertStatement.setString(7, proveedores);
                        insertStatement.setString(8, tipoContrato);
                        insertStatement.executeUpdate();
                        } catch (Exception e) {
                            System.err.println("En la fila " + i + e.getMessage());
                        }
                    }

                    insertStatement.close();
                    System.out.println("Datos insertados en la tabla contratos con éxito");

                    XMLExporter.exportDataToXML(conn);

                    //Creamos un nuevo documento
                   Document newDocument = builder.newDocument();
                   Element rootElement = newDocument.createElement("Contratos");
                    rootElement.setAttribute("xmlns:ss", "http://schemas.openxmlformats.org/spreadsheetml/2006/main");
                    newDocument.appendChild(rootElement);

                    for (int i = 1; i < contratoNodes.getLength(); i++) {
                        Node rowNode = contratoNodes.item(i);
                        if (rowNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element contratoElem = (Element) rowNode;

                            // Clona el elemento y sus descendientes al nuevo documento
                            Element newContratoElem = (Element) newDocument.importNode(contratoElem, true);

                            // Elimina la información del "TIPO DE CONTRATO" en el nuevo elemento
                            Node tipoContratoNode = newContratoElem.getElementsByTagName("tipo_contrato").item(7);
                            if (tipoContratoNode != null) {
                                newContratoElem.removeChild(tipoContratoNode);
                            }

                            rootElement.appendChild(newContratoElem);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

    }
        }

class XMLExporter {
    public static void exportDataToXML(Connection connection) {
        try {
            // Crear un nuevo documento XML
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            // Consulta SQL para obtener todos los datos de la tabla
            String selectQuery = "SELECT NIF, ADJUDICATARIO, OBJETO_GENERICO, OBJETO, FECHA_ADJUDICACION, IMPORTE, PROVEEDORES FROM CONTRATOS";
            PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
            ResultSet resultSet = selectStatement.executeQuery();

            // Elemento raíz del nuevo documento XML
            Element rootElement = document.createElement("Contratos");
            document.appendChild(rootElement);

            // Recorrer los resultados de la consulta y agregarlos al documento XML
            while (resultSet.next()) {
                Element contratoElement = document.createElement("Contrato");
                rootElement.appendChild(contratoElement);

                // Agregar datos como elementos dentro de cada contrato
                addDataElement(document, contratoElement, "NIF", resultSet.getString("NIF").replace("\n", "&#10;"));
                addDataElement(document, contratoElement, "Adjudicatario", resultSet.getString("ADJUDICATARIO").replace("\n", "&#10;"));
                addDataElement(document, contratoElement, "ObjetoGenerico", resultSet.getString("OBJETO_GENERICO").replace("\n", "&#10;"));
                addDataElement(document, contratoElement, "FechaAdjudicacion", resultSet.getString("FECHA_ADJUDICACION").replace("\n", "&#10;"));
                addDataElement(document, contratoElement, "Importe", resultSet.getString("IMPORTE").replace("\n", "&#10;"));
                addDataElement(document, contratoElement, "Proveedores", resultSet.getString("PROVEEDORES").replace("\n", "&#10;"));
            }

            // Especifica la ruta completa para el archivo "output.xml"
            String outputXmlPath = "C:\\Users\\Usuario\\Documents\\IdeaProjects\\TrabajoVoluntarioAccesos\\src\\nuevo.xml";

            // Guardar el document XML en el archivo
            saveXMLToFile(document, outputXmlPath);
            resultSet.close();
            selectStatement.close();
        } catch (SQLException | ParserConfigurationException e) {
            e.printStackTrace();
        }

        
    }

    private static void addDataElement(Document document, Element parentElement, String tagName, String textContent) {
        Element element = document.createElement(tagName);
        element.appendChild(document.createTextNode(textContent));
        parentElement.appendChild(element);
    }

    private static void saveXMLToFile(Document document, String filePath) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);

            // Guardar el documento en un archivo
            StreamResult result = new StreamResult(new File(filePath));
            transformer.transform(source, result);

            System.out.println("Datos exportados a " + filePath);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

}

