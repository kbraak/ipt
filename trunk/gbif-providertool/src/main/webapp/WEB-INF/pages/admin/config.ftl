<head>
    <title><@s.text name='config.heading'/></title>
    <meta name="menu" content="MainMenu"/>
</head>

<h1><@s.text name='config.heading'/></h1>

<@s.form id="providerCfg" action="saveConfig" method="post">

  <fieldset>
    <legend><@s.text name="config.metadata"/></legend>
	<@s.textfield key="config.title" required="true" cssClass="text xlarge"/>
    <div class="right">
		<#if (config.location)?? && cfg.googleMapsApiKey??>
			<a href="http://maps.google.de/maps?f=s&ie=UTF8&ll=${config.location.latitude!0},${config.location.longitude!0}&t=h&z=15"><img style="padding-right:15px" src="http://maps.google.com/staticmap?center=${config.location.latitude!0},${config.location.longitude!0}&zoom=5&size=95x95&key=${cfg.googleMapsApiKey}" /></a>	
		</#if>
    </div>
	<@s.textfield key="config.link" required="true" cssClass="text large"/>
    <div>
        <div class="left">
			<@s.textfield key="config.contactName" required="true" cssClass="text medium"/>
        </div>
        <div class="left">
			<@s.textfield key="config.contactEmail" required="true" cssClass="text medium"/>
        </div>
        <div class="left">
			<@s.textfield key="config.location.latitude" required="false" cssClass="text small"/>
        </div>
        <div>
			<@s.textfield key="config.location.longitude" required="false" cssClass="text small"/>
        </div>

	    
    
	<@s.textfield key="config.emlUrl" required="false" cssClass="text xlarge"/>
	<@s.textfield key="config.descriptionImage" required="false" cssClass="text xlarge"/>
	<@s.textarea key="config.description" cssClass="text xlarge"/>
  </fieldset>

<br/>
<br/>

  <fieldset>
    <legend><@s.text name="config.settings"/></legend>
	<@s.textfield key="config.baseUrl" required="true" cssClass="text xlarge"/>
	<@s.textfield key="config.geoserverUrl" required="true" cssClass="text xlarge"/>
	<@s.textfield key="config.dataDir" required="false" cssClass="text xlarge"/>
	<@s.textfield key="config.googleMapsApiKey" required="false" cssClass="text xlarge"/>
	<div>&nbsp;&nbsp;<a href="http://code.google.com/apis/maps/signup.html">Get Google Maps API key</a></div>
  </fieldset>
  
    <@s.submit cssClass="button" name="save" key="button.save" theme="simple"/>
    <@s.submit cssClass="button" name="cancel" key="button.cancel" theme="simple"/>
</@s.form>
