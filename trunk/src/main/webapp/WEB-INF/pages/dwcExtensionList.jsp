<%@ include file="/common/taglibs.jsp"%>

<head>
    <title><fmt:message key="occResourceList.title"/></title>
    <meta name="heading" content="<fmt:message key='occResourceList.heading'/>"/>
    <meta name="menu" content="OccResourceMenu"/>
</head>

<display:table name="extensions" class="table" requestURI="" export="true" pagesize="25">
    <display:column property="name" sortable="true" titleKey="extension.name" 
    	href="link" media="html" />
    <display:column property="namespace" sortable="true" titleKey="extension.namespace"/>
    <display:column property="id" sortable="true" titleKey="extension.propertyCount"
    	href="extension.html" media="html" paramId="id" paramProperty="id" />

    <display:setProperty name="paging.banner.item_name"><fmt:message key="extensionList.extension"/></display:setProperty>
    <display:setProperty name="paging.banner.items_name"><fmt:message key="extensionList.extensions"/></display:setProperty>
</display:table>

<script type="text/javascript">
    highlightTableRows("extensions");
</script>
