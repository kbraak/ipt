<!-- Represents source data and mapping data sections on resource overview page -->
<div class="my-3 p-3 bg-body rounded shadow-sm" id="sources">
    <div class="titleOverview">
        <h6 class="border-bottom pb-2 mb-2 mx-md-4 mx-2 text-success">
            <#assign sourcesInfo>
                <@s.text name='manage.overview.source.description1'/>&nbsp;<@s.text name='manage.overview.source.description2'/>&nbsp;<@s.text name='manage.overview.source.description3'><@s.param><@s.text name='button.add'/></@s.param></@s.text></br></br><@s.text name='manage.overview.source.description4'><@s.param><@s.text name="button.connectDB"/></@s.param></@s.text></br></br><@s.text name='manage.overview.source.description5'/>
            </#assign>
            <@popoverTextInfo sourcesInfo/>

            <@s.text name='manage.overview.source.data'/>
        </h6>

        <div class="row">
            <div class="col-xl-9 order-xl-last">
                <div class="mx-md-4 mx-2">
                    <p class="text-muted">
                        <@s.text name='manage.overview.source.intro'/>
                    </p>
                    <div class="details twenty_bottom">
                        <#if sourcesModifiedSinceLastPublication>
                            <@s.text name='manage.home.last.modified'/> ${resource.getSourcesModified()?date?string.medium!}
                        <#elseif resource.lastPublished??>
                            <@s.text name="manage.overview.notModified"/>
                        </#if>
                    </div>

                    <#if (resource.sources?size>0)>
                        <div class="details">
                            <table class="table table-sm table-borderless">
                                <#list resource.sources as src>
                                    <tr>
                                        <#if src.isFileSource()>
                                            <th>${src.name} <@s.text name='manage.overview.source.file'/></th>
                                            <td>${src.fileSizeFormatted},&nbsp;${src.rows}&nbsp;<@s.text name='manage.overview.source.rows'/>,&nbsp;${src.getColumns()}&nbsp;<@s.text name='manage.overview.source.columns'/>.&nbsp;${(src.lastModified?date?string.medium)!}<#if !src.readable>&nbsp;<img src="${baseURL}/images/warning.gif"/></#if></td>
                                        <#elseif src.isExcelSource()>
                                            <th>${src.name} <@s.text name='manage.overview.source.excel'/></th>
                                            <td>${src.fileSizeFormatted},&nbsp;${src.rows}&nbsp;<@s.text name='manage.overview.source.rows'/>,&nbsp;${src.getColumns()}&nbsp;<@s.text name='manage.overview.source.columns'/>.&nbsp;${(src.lastModified?date?string.medium)!}<#if !src.readable>&nbsp;<img src="${baseURL}/images/warning.gif"/></#if></td>
                                        <#else>
                                            <th>${src.name} <@s.text name='manage.overview.source.sql'/></th>
                                            <td>db=${src.database!"..."},&nbsp;${src.columns}&nbsp;<@s.text name='manage.overview.source.columns'/>.<#if !src.readable>&nbsp;<img src="${baseURL}/images/warning.gif"/></#if></td>
                                        </#if>
                                        <td>
                                            <a class="btn btn-outline-success ignore-link-color" role="button" href="source.do?r=${resource.shortname}&id=${src.name}">
                                                <@s.text name='button.edit'/>
                                            </a>
                                        </td>
                                    </tr>
                                </#list>
                            </table>
                        </div>
                    </#if>
                </div>
            </div>

            <div class="col-xl-3">
                <div class="mx-md-4 mx-2">
                    <form action='addsource.do' method='post' enctype="multipart/form-data">
                        <input name="r" type="hidden" value="${resource.shortname}"/>
                        <input name="validate" type="hidden" value="false"/>

                        <div class="row">
                            <div class="col-12">
                                <@s.file name="file" cssClass="form-control my-1" key="manage.resource.create.file"/>
                            </div>
                            <div class="col-12">
                                <@s.submit name="add" cssClass="btn btn-outline-success my-1" key="button.connectDB"/>
                                <@s.submit name="clear" cssClass="btn btn-outline-secondary my-1" key="button.clear"/>
                                <@s.submit name="cancel" cssClass="btn btn-outline-secondary my-1" cssStyle="display: none" key="button.cancel" method="cancelOverwrite"/>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>

    </div>

</div>

<div class="my-3 p-3 bg-body rounded shadow-sm" id="mappings">
    <h6 class="border-bottom pb-2 mb-2 mx-md-4 mx-2 text-success">
        <#assign mappingsInfo>
            <@s.text name='manage.overview.source.description1'/>&nbsp;<@s.text name='manage.overview.source.description2'/>&nbsp;<@s.text name='manage.overview.source.description3'><@s.param><@s.text name='button.add'/></@s.param></@s.text></br></br><@s.text name='manage.overview.source.description4'><@s.param><@s.text name="button.connectDB"/></@s.param></@s.text></br></br><@s.text name='manage.overview.source.description5'/><@s.text name='manage.overview.DwC.Mappings.coretype.description1'/></br></br><@s.text name='manage.overview.DwC.Mappings.coretype.description2'/></br></br><@s.text name='manage.overview.DwC.Mappings.coretype.description3'/></br></br><@s.text name='manage.overview.DwC.Mappings.coretype.description4'/>
        </#assign>
        <@popoverTextInfo mappingsInfo/>

        <@s.text name='manage.overview.DwC.Mappings'/>
    </h6>

    <div class="row">
        <div class="col-sm-9 order-sm-last">
            <div class="mx-md-4 mx-2">
                <p class="text-muted">
                    <@s.text name='manage.overview.DwC.Mappings.description'/>
                </p>

                <div class="details twenty_bottom">
                    <#if mappingsModifiedSinceLastPublication>
                        <@s.text name='manage.home.last.modified'/> ${resource.getMappingsModified()?date?string.medium!}
                    <#elseif resource.lastPublished??>
                        <@s.text name="manage.overview.notModified"/>
                    </#if>
                </div>

                <#if resource.coreRowType?has_content>
                    <div class="details">
                        <div class="mapping_head"><@s.text name='manage.overview.DwC.Mappings.cores.select'/></div>
                        <table class="table table-sm table-borderless">
                            <#list resource.getMappings(resource.coreRowType) as m>
                                <tr <#if m_index==0>class="mapping_row"</#if>>
                                    <th><#if m_index==0>${m.extension.title}</#if></th>
                                    <td>${m.fields?size} <@s.text name='manage.overview.DwC.Mappings.terms'/> ${(m.source.name)!}.&nbsp;${(m.lastModified?date?string.medium)!}</td>
                                    <td>
                                        <a class="btn btn-outline-secondary ignore-link-color" role="button" href="mappingPeek.do?r=${resource.shortname}&id=${m.extension.rowType?url}&mid=${m_index}">
                                            <@s.text name='button.preview'/>
                                        </a>
                                        <a class="btn btn-outline-success ignore-link-color" role="button" href="mapping.do?r=${resource.shortname}&id=${m.extension.rowType?url}&mid=${m_index}">
                                            <@s.text name='button.edit'/>
                                        </a>

                                    </td>
                                </tr>
                            </#list>
                        </table>
                        <#if (resource.getMappedExtensions()?size > 1)>
                            <div class="mapping_head twenty_top"><@s.text name='manage.overview.DwC.Mappings.extensions.select'/></div>
                            <table class="table table-sm table-borderless">
                                <#list resource.getMappedExtensions() as ext>
                                    <#if ext.rowType != resource.coreRowType>
                                        <#list resource.getMappings(ext.rowType) as m>
                                            <tr <#if m_index==0>class="mapping_row"</#if>>
                                                <th><#if m_index==0>${ext.title}</#if></th>
                                                <td>${m.fields?size} <@s.text name='manage.overview.DwC.Mappings.terms'/> ${(m.source.name)!}.&nbsp;${(m.lastModified?date?string.medium)!}</td>
                                                <td>
                                                    <!-- preview icon is taken from Gentleface Toolbar Icon Set available from http://gentleface.com/free_icon_set.html licensed under CC-BY -->
                                                    <a class="btn btn-outline-secondary" role="button" href="mappingPeek.do?r=${resource.shortname}&id=${ext.rowType?url}&mid=${m_index}">
                                                        <@s.text name='button.preview'/>
                                                    </a>
                                                    <a class="btn btn-outline-success" role="button" href="mapping.do?r=${resource.shortname}&id=${ext.rowType?url}&mid=${m_index}">
                                                        <@s.text name='button.edit'/>
                                                    </a>
                                                </td>
                                            </tr>
                                        </#list>
                                    </#if>
                                </#list>
                            </table>
                        </#if>
                    </div>
                </#if>
            </div>
        </div>

        <div class="col-sm-3">
            <div class="mx-md-4 mx-2">
                <#if (potentialCores?size>0)>
                    <form action='mapping.do' method='post'>
                        <input name="r" type="hidden" value="${resource.shortname}"/>
                        <select name="id" class="form-select my-1" id="rowType" size="1">
                            <optgroup label="<@s.text name='manage.overview.DwC.Mappings.cores.select'/>">
                                <#list potentialCores as c>
                                    <#if c?has_content>
                                        <option value="${c.rowType}">${c.title}</option>
                                    </#if>
                                </#list>
                            </optgroup>
                            <#if (potentialExtensions?size>0)>
                                <optgroup label="<@s.text name='manage.overview.DwC.Mappings.extensions.select'/>">
                                    <#list potentialExtensions as e>
                                        <#if e?has_content>
                                            <option value="${e.rowType}">${e.title}</option>
                                        </#if>
                                    </#list>
                                </optgroup>
                            </#if>
                        </select>
                        <@s.submit name="add" cssClass="btn btn-outline-success my-1" key="button.add"/>
                    </form>
                <#else>
                    <div class="d-flex justify-content-start">
                        <select class="form-select">
                            <option value=""></option>
                        </select>
                        <@popoverPropertyWarning "manage.overview.DwC.Mappings.cantdo"/>
                    </div>
                </#if>
            </div>
        </div>
    </div>
</div>
