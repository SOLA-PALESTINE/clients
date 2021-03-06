/**
 * ******************************************************************************************
 * Copyright (C) 2012 - Food and Agriculture Organization of the United Nations (FAO). All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,this list of conditions
 * and the following disclaimer. 2. Redistributions in binary form must reproduce the above
 * copyright notice,this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution. 3. Neither the name of FAO nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sola.clients.swing.gis.ui.controlsbundle;

import java.util.List;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.swing.extended.exception.InitializeLayerException;
import org.sola.clients.swing.gis.beans.TransactionCadastreChangeBean;
import org.sola.clients.swing.gis.data.PojoDataAccess;
import org.sola.clients.swing.gis.layer.CadastreChangeNewCadastreObjectLayer;
import org.sola.clients.swing.gis.layer.CadastreChangeNewSurveyPointLayer;
import org.sola.clients.swing.gis.layer.CadastreChangeTargetCadastreObjectLayer;
import org.sola.clients.swing.gis.mapaction.CadastreChangeNewCadastreObjectListFormShow;
import org.sola.clients.swing.gis.mapaction.CadastreChangePointSurveyListFormShow;
import org.sola.clients.swing.gis.tool.CadastreBoundarySelectTool;
import org.sola.clients.swing.gis.tool.CadastreChangeNewParcelTool;
import org.sola.clients.swing.gis.tool.CadastreChangeNodeTool;
import org.sola.clients.swing.gis.tool.CadastreChangeSelectParcelTool;
import org.sola.webservices.transferobjects.cadastre.CadastreObjectTO;

/**
 * A control bundle that is used for cadastre change process. The necessary tools and layers are
 * added in the bundle.
 *
 * @author Elton Manoku
 */
public final class ControlsBundleForCadastreChange extends ControlsBundleForTransaction {

    private TransactionCadastreChangeBean transactionBean;
    private CadastreChangeTargetCadastreObjectLayer targetParcelsLayer = null;
    private CadastreChangeNewCadastreObjectLayer newCadastreObjectLayer = null;
    private CadastreChangeNewSurveyPointLayer newPointsLayer = null;
    private String applicationNumber = "";

    /**
     * Constructor. It sets up the bundle by adding layers and tools that are relevant. Finally, it
     * zooms in the interested zone. The interested zone is defined in the following order: <br/> If
     * bean has survey points it is zoomed there, otherwise if baUnitId is present it is zoomed
     * there else it is zoomed in the application location.
     *
     * @param applicationNumber The application number that is used for generating new property
     * identifiers
     * @param transactionBean The transaction bean. If this is already populated it means the
     * transaction is being opened again for change.
     * @param baUnitId Id of the property that is defined in the application as a target for this
     * cadastre change.
     * @param applicationLocation Location of application that starts the cadastre change
     */
    public ControlsBundleForCadastreChange(
            String applicationNumber,
            TransactionCadastreChangeBean transactionBean,
            String baUnitId,
            byte[] applicationLocation) {
        super();
        this.applicationNumber = applicationNumber;
        this.transactionBean = transactionBean;
        if (this.transactionBean == null) {
            this.transactionBean = new TransactionCadastreChangeBean();
        }

        this.Setup(PojoDataAccess.getInstance());

        if (!this.transactionIsStarted()) {
            this.setTargetParcelsByBaUnit(baUnitId);
        }
        this.zoomToInterestingArea(null, applicationLocation);
    }

    /**
     * Gets if the transaction is already started before.
     *
     * @return True if the transaction was already started and now is read back for modifications
     */
    protected boolean transactionIsStarted() {
        return (this.newPointsLayer.getFeatureCollection().size() > 0
                || this.targetParcelsLayer.getFeatureCollection().size() > 0);
    }

    /**
     * It zooms to the interesting area which is the area where the cadastre changes is happening
     *
     * @param interestingArea
     * @param applicationLocation
     */
    @Override
    protected void zoomToInterestingArea(
            ReferencedEnvelope interestingArea, byte[] applicationLocation) {
        ReferencedEnvelope boundsToZoom = null;
        if (this.newPointsLayer.getFeatureCollection().size() > 0) {
            boundsToZoom = this.newPointsLayer.getFeatureCollection().getBounds();
        } else if (this.targetParcelsLayer.getFeatureCollection().size() > 0) {
            boundsToZoom = this.targetParcelsLayer.getFeatureCollection().getBounds();
        }
        super.zoomToInterestingArea(boundsToZoom, applicationLocation);
    }

    @Override
    public TransactionCadastreChangeBean getTransactionBean() {
        transactionBean.setCadastreObjectList(
                this.newCadastreObjectLayer.getCadastreObjectList());
        transactionBean.setSurveyPointList(this.newPointsLayer.getSurveyPointList());
        transactionBean.setCadastreObjectTargetList(
                this.targetParcelsLayer.getCadastreObjectTargetList());
        return transactionBean;
    }

    @Override
    protected void addLayers() throws InitializeLayerException {
        super.addLayers();
        this.targetParcelsLayer = new CadastreChangeTargetCadastreObjectLayer();
        this.getMap().addLayer(targetParcelsLayer);

        this.newCadastreObjectLayer = new CadastreChangeNewCadastreObjectLayer(
                this.applicationNumber);
        this.getMap().addLayer(newCadastreObjectLayer);

        this.newPointsLayer = new CadastreChangeNewSurveyPointLayer(this.newCadastreObjectLayer);
        this.getMap().addLayer(newPointsLayer);

        this.targetParcelsLayer.setCadastreObjectTargetList(
                transactionBean.getCadastreObjectTargetList());

        this.newPointsLayer.setSurveyPointList(this.transactionBean.getSurveyPointList());

        this.newCadastreObjectLayer.setCadastreObjectList(
                this.transactionBean.getCadastreObjectList());
    }

    @Override
    protected void addToolsAndCommands() {
        CadastreChangeSelectParcelTool selectParcelTool =
                new CadastreChangeSelectParcelTool(this.getPojoDataAccess());
        selectParcelTool.setTargetParcelsLayer(targetParcelsLayer);
        this.getMap().addTool(selectParcelTool, this.getToolbar(), true);

        this.getMap().addMapAction(
                new CadastreChangePointSurveyListFormShow(
                this.getMap(), this.newPointsLayer.getHostForm()),
                this.getToolbar(),
                true);

        CadastreChangeNodeTool nodelinkingTool = new CadastreChangeNodeTool(newPointsLayer);
        nodelinkingTool.getTargetSnappingLayers().add(this.targetParcelsLayer);
        this.getMap().addTool(nodelinkingTool, this.getToolbar(), true);

        CadastreChangeNewParcelTool newParcelTool =
                new CadastreChangeNewParcelTool(this.newCadastreObjectLayer);
        newParcelTool.getTargetSnappingLayers().add(newPointsLayer);
        this.getMap().addTool(newParcelTool, this.getToolbar(), true);

        this.getMap().addMapAction(new CadastreChangeNewCadastreObjectListFormShow(
                this.getMap(), this.newCadastreObjectLayer.getHostForm()),
                this.getToolbar(),
                true);
        CadastreBoundarySelectTool cadastreBoundarySelectTool =
                new CadastreBoundarySelectTool(
                this.cadastreBoundaryPointLayer,
                this.newCadastreObjectLayer,
                this.newCadastreObjectLayer.getVerticesLayer());
        this.getMap().addTool(cadastreBoundarySelectTool, this.getToolbar(), true);
        super.addToolsAndCommands();
        this.cadastreBoundaryEditTool.setTargetLayer(this.newCadastreObjectLayer);
        this.cadastreBoundaryEditTool.getTargetSnappingLayers().add(this.targetParcelsLayer);

    }

    /**
     * Sets cadastre objects that are related with the baUnitId
     *
     * @param baUnitId
     */
    public void setTargetParcelsByBaUnit(String baUnitId) {
        List<CadastreObjectTO> cadastreObjects =
                this.getPojoDataAccess().getCadastreService().getCadastreObjectsByBaUnit(baUnitId);
        this.addCadastreObjectsInLayer(targetParcelsLayer, cadastreObjects);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        this.getMap().getMapActionByName(CadastreChangeSelectParcelTool.NAME).setEnabled(!readOnly);
        this.getMap().getMapActionByName(
                CadastreChangePointSurveyListFormShow.MAPACTION_NAME).setEnabled(!readOnly);
        this.getMap().getMapActionByName(CadastreChangeNodeTool.NAME).setEnabled(!readOnly);
        this.getMap().getMapActionByName(CadastreChangeNewParcelTool.NAME).setEnabled(!readOnly);
        this.getMap().getMapActionByName(
                CadastreChangeNewCadastreObjectListFormShow.MAPACTION_NAME).setEnabled(!readOnly);
    }
}
