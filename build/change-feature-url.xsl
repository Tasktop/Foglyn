<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1">

<xsl:output method="xml" />

<xsl:template match="node()|@*">
    <xsl:copy>
        <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
</xsl:template>

<xsl:param name="url" />

<xsl:template match="feature/url/update">
    <update>
        <xsl:attribute name="url">
            <xsl:value-of select="$url" />
        </xsl:attribute>
    </update>
</xsl:template>

</xsl:stylesheet>	
