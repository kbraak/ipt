<#macro publish resource>
    <form action='publish.do' method='post'>
        <input name="r" type="hidden" value="${resource.shortname}"/>

        <textarea id="summary" name="summary" cols="40" rows="5" style="display: none"></textarea>

        <!-- resources cannot be published if the mandatory metadata is missing -->
        <#if missingMetadata>
            <@s.submit id="publishButton" cssClass="btn btn-outline-secondary" name="publish" key="button.publish" disabled="true"/>
            <@popoverPropertyWarning "manage.overview.published.missing.metadata"/>


        <!-- resources that are already registered cannot be re-published if they haven't been assigned a GBIF-supported license -->
        <#elseif resource.isRegistered() && !resource.isAssignedGBIFSupportedLicense()>
            <@s.submit id="publishButton" cssClass="btn btn-outline-secondary" name="publish" key="button.publish" disabled="true"/>
            <@popoverPropertyWarning "manage.overview.prevented.resource.publishing.noGBIFLicense"/>


        <!-- previously published resources without a DOI, or that haven't been registered yet can be republished whenever by any manager -->
        <#elseif resource.lastPublished?? && resource.identifierStatus == "UNRESERVED" && resource.status != "REGISTERED">
            <@s.submit cssClass="confirmPublishMinorVersion btn btn-outline-success" id="publishButton" name="publish" key="button.publish"/>


        <!-- resources with a reserved DOI, existing registered DOI, or registered with GBIF can only be republished by managers with registration rights -->
        <#elseif (resource.identifierStatus == "PUBLIC_PENDING_PUBLICATION")
        || (resource.identifierStatus == "PUBLIC" && resource.isAlreadyAssignedDoi())
        || resource.status == "REGISTERED">
            <!-- the user must have registration rights -->
            <#if !currentUser.hasRegistrationRights()>
                <@s.submit id="publishButton" cssClass="btn btn-outline-secondary" name="publish" key="button.publish" disabled="true"/>
                <#assign popover1>
                    <@s.text name="manage.resource.status.publication.forbidden"/>
                    &nbsp;<@s.text name="manage.resource.role.change"/>
                </#assign>
                <@popoverTextWarning popover1/>

            <!-- an organisation with DOI account be activated (if resource has a reserved DOI or existing registered DOI) -->
            <#elseif ((resource.identifierStatus == "PUBLIC_PENDING_PUBLICATION" && resource.isAlreadyAssignedDoi())
            || (resource.identifierStatus == "PUBLIC" && resource.isAlreadyAssignedDoi()))
            && !organisationWithPrimaryDoiAccount??>
                <@s.submit id="publishButton" cssClass="btn btn-outline-secondary" name="publish" key="button.publish" disabled="true"/>
                <@popoverPropertyWarning "manage.resource.status.publication.forbidden.account.missing"/>

            <!-- when a DOI is reserved.. -->
            <#elseif resource.identifierStatus == "PUBLIC_PENDING_PUBLICATION">
                <!-- and the resource has no existing DOI and its status is private..  -->
                <#if !resource.isAlreadyAssignedDoi() && resource.status == "PRIVATE">
                    <!-- and the resource has never been published before, the first publication is a new major version -->
                    <#if !resource.lastPublished??>
                        <@s.submit cssClass="confirmPublishMajorVersionWithoutDOI btn btn-outline-success" id="publishButton" name="publish" key="button.publish"/>
                        <@popoverPropertyWarning "manage.overview.publishing.doi.register.prevented.notPublic"/>

                    <!-- and the resource has been published before, the next publication is a new minor version -->
                    <#else>
                        <@s.submit cssClass="confirmPublishMinorVersion btn btn-outline-success" id="publishButton" name="publish" key="button.publish"/>
                        <@popoverPropertyInfo "manage.overview.publishing.doi.register.prevented.notPublic"/>
                    </#if>


                <!-- and its status is public (or registered), its reserved DOI can be registered during next publication  -->
                <#elseif resource.status == "PUBLIC" || resource.status == "REGISTERED">
                    <@s.submit cssClass="confirmPublishMajorVersion btn btn-outline-success" id="publishButton" name="publish" key="button.publish"/>
                    <@popoverPropertyInfo "manage.overview.publishing.doi.register.help"/>

                </#if>

            <!-- publishing a new minor version -->
            <#elseif resource.identifierStatus == "PUBLIC" && resource.isAlreadyAssignedDoi()>
                <@s.submit cssClass="confirmPublishMinorVersion btn btn-outline-success" id="publishButton" name="publish" key="button.publish"/>

            <!-- publishing a new version registered with GBIF -->
            <#elseif resource.status == "REGISTERED">
                <@s.submit cssClass="confirmPublishMinorVersion btn btn-outline-success" id="publishButton" name="publish" key="button.publish"/>
            </#if>


        <!-- first time any resource not assigned a DOI is published is always new major version -->
        <#elseif !resource.lastPublished?? && resource.identifierStatus == "UNRESERVED">
            <@s.submit cssClass="confirmPublishMajorVersionWithoutDOI btn btn-outline-success" id="publishButton" name="publish" key="button.publish"/>


        <!-- otherwise prevent publication from happening just to be safe -->
        <#else>
            <@s.submit id="publishButton" cssClass="btn btn-outline-secondary" name="publish" key="button.publish" disabled="true"/>
        </#if>

        <br/>
        <br/>
    </form>
</#macro>
