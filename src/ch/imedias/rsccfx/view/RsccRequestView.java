package ch.imedias.rsccfx.view;

import ch.imedias.rsccfx.localization.Strings;
import ch.imedias.rsccfx.model.Rscc;
import ch.imedias.rsccfx.model.util.KeyUtil;
import ch.imedias.rsccfx.view.util.KeyTextField;
import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import java.io.InputStream;
import java.util.logging.Logger;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Defines all elements shown in the request section.
 */
public class RsccRequestView extends BorderPane {
  private static final Logger LOGGER =
      Logger.getLogger(RsccRequestView.class.getName());

  private static final double BUTTON_SIZE = 50d;
  private static final double GENERATEDKEYFLD_HEIGHT = 60d;

  final HeaderView headerView;

  final Label titleLbl = new Label();
  final Label descriptionLbl = new Label();
  final Label supporterDescriptionLbl = new Label();
  final Label statusLbl = new Label();

  GridPane supporterGrid = new GridPane();
  final GridPane keyGenerationInnerPane = new GridPane();

  final HBox statusBox = new HBox();
  final HBox predefinedAdressesInnerBox = new HBox();
  final VBox contentBox = new VBox();

  final TitledPane predefinedAddressesTitledPane = new TitledPane();
  final TitledPane keyGenerationTitledPane = new TitledPane();

  final ScrollPane scrollPane = new ScrollPane();

  final KeyTextField generatedKeyFld = new KeyTextField();

  final Button reloadKeyBtn = new Button();

  private final Rscc model;
  private final Strings strings = new Strings();
  private final KeyUtil keyUtil;

  Image reloadImg;

  ImageView reloadImgView;


  /**
   * Initializes all the GUI components needed to generate the key the supporter needs.
   *
   * @param model the model to handle the data.
   */
  public RsccRequestView(Rscc model) {
    this.model = model;
    headerView = new HeaderView(model);
    this.keyUtil = model.getKeyUtil();
    SvgImageLoaderFactory.install();
    initFieldData();
    layoutForm();
    layoutKeyGenerationPane();
    layoutSupporterPane();
    bindFieldsToModel();
  }

  private void initFieldData() {
    // populate fields which require initial data
    titleLbl.setText(strings.requestTitleLbl);
    descriptionLbl.setText(strings.requestDescriptionLbl);
    generatedKeyFld.setText(strings.requestGeneratedKeyFld);
    supporterDescriptionLbl.setText(strings.requestSupporterDescriptionLbl);
    keyGenerationTitledPane.setText(strings.requestKeyGeneratorPane);
    predefinedAddressesTitledPane.setText(strings.requestPredefinedAdressessPane);
    statusLbl.setText("");

    InputStream reloadImagePath = getClass().getClassLoader()
        .getResourceAsStream("images/reload.svg");
    reloadImg = new Image(reloadImagePath);
    reloadImgView = new ImageView(reloadImg);

    reloadKeyBtn.setGraphic(reloadImgView);
  }

  private void layoutForm() {
    //setup layout (aka setup specific pane etc.)
    keyGenerationTitledPane.setExpanded(true);
    keyGenerationTitledPane.setId("keyGenerationTitledPane");

    predefinedAddressesTitledPane.setExpanded(false);
    predefinedAddressesTitledPane.setId("predefinedAddressesTitledPane");

    titleLbl.getStyleClass().add("titleLbl");

    descriptionLbl.getStyleClass().add("descriptionLbl"); // TODO: Styling

    supporterDescriptionLbl.getStyleClass().add("supporterDescriptionLbl");

    statusBox.getStyleClass().add("statusBox");
    statusBox.getChildren().addAll(statusLbl);
    statusLbl.getStyleClass().add("statusLbl");

    //generatedKeyFld.setPrefHeight(GENERATEDKEYFLD_HEIGHT); // FIXME: Has this to be in the CSS?
    generatedKeyFld.setEditable(false); // FIXME: Has this to be in the CSS?
    generatedKeyFld.setId("generatedKeyFld");

    reloadImgView.fitWidthProperty().set(BUTTON_SIZE); // FIXME: Has this to be in the CSS?
    reloadImgView.fitHeightProperty().set(BUTTON_SIZE); // FIXME: Has this to be in the CSS?
    reloadImgView.setPreserveRatio(true);

    reloadKeyBtn.setPrefWidth(BUTTON_SIZE); // FIXME: Has this to be in the CSS?
    reloadKeyBtn.setPrefHeight(BUTTON_SIZE); // FIXME: Has this to be in the CSS?
    reloadKeyBtn.setId("reloadKeyBtn");

    contentBox.getChildren().addAll(keyGenerationTitledPane, keyGenerationInnerPane,
        predefinedAddressesTitledPane);
    descriptionLbl.getStyleClass().add("descriptionLbl"); // TODO: Styling

    VBox.setVgrow(keyGenerationInnerPane, Priority.ALWAYS);
    VBox.setVgrow(predefinedAdressesInnerBox, Priority.ALWAYS);

    setTop(headerView);
    setCenter(contentBox);
  }

  private void layoutSupporterPane() {
    predefinedAdressesInnerBox.getChildren().addAll(scrollPane, supporterDescriptionLbl);

    scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scrollPane.setContent(supporterGrid);

    // add column constraints
    ColumnConstraints col1 = new ColumnConstraints();
    ColumnConstraints col2 = new ColumnConstraints();
    ColumnConstraints col3 = new ColumnConstraints();
    supporterGrid.getColumnConstraints().addAll(col1, col2, col3);
    int amountOfColumns = supporterGrid.getColumnConstraints().size();
    int columnPercentWidth = 100 / amountOfColumns;
    col1.setPercentWidth(columnPercentWidth);
    col2.setPercentWidth(columnPercentWidth);
    col3.setPercentWidth(columnPercentWidth);
  }

  private void layoutKeyGenerationPane() {
    // set elements
    GridPane.setConstraints(generatedKeyFld, 0, 0);
    GridPane.setConstraints(reloadKeyBtn, 1, 0);
    GridPane.setConstraints(titleLbl, 2, 0);
    GridPane.setConstraints(descriptionLbl, 2, 1);
    GridPane.setConstraints(statusBox, 0, 2);

    GridPane.setRowSpan(generatedKeyFld, 2);
    GridPane.setRowSpan(reloadKeyBtn, 2);
    GridPane.setColumnSpan(statusBox, 3);

    keyGenerationInnerPane.getChildren().addAll(generatedKeyFld, reloadKeyBtn, titleLbl,
        descriptionLbl, statusBox);

    // initial styling
    keyGenerationInnerPane.getChildren().stream()
        .forEach(node -> {
          GridPane.setVgrow(node, Priority.ALWAYS);
          GridPane.setHgrow(node, Priority.ALWAYS);
          GridPane.setValignment(node, VPos.CENTER);
          GridPane.setHalignment(node, HPos.CENTER);
          GridPane.setMargin(node, new Insets(10));
          keyGenerationInnerPane.setAlignment(Pos.CENTER);

            }
        );

    // column division
    ColumnConstraints col1 = new ColumnConstraints();
    col1.setPercentWidth(40);
    ColumnConstraints col2 = new ColumnConstraints();
    col2.setPercentWidth(10);
    ColumnConstraints col3 = new ColumnConstraints();
    col3.setPercentWidth(50);
    keyGenerationInnerPane.getColumnConstraints().addAll(col1,col2,col3);

    // special styling
    GridPane.setVgrow(statusBox, Priority.NEVER);
    GridPane.setValignment(titleLbl,VPos.BOTTOM);
    GridPane.setValignment(descriptionLbl,VPos.TOP);
    GridPane.setMargin(titleLbl, new Insets(0));
    GridPane.setMargin(descriptionLbl, new Insets(0));
    keyGenerationInnerPane.setPadding(new Insets(10));

  }

  private void bindFieldsToModel() {
    // make bindings to the model
    generatedKeyFld.textProperty().bind(keyUtil.formattedKeyProperty());
  }
}
