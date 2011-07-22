<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:h="http://www.w3.org/1999/xhtml"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="#all">

    <xsl:param name="suffix">.fpage</xsl:param>
    <xsl:param name="outdir">out/</xsl:param>
    <xsl:param name="helpPrefix">http://help.eclipse.org/galileo/topic</xsl:param>

    <xsl:variable name="metadata">
        menu="user-guide"
        aux_stylesheet="user-guide.css"
        content_class="wide"
        dont_include_news=true
        dont_include_links=true
        display_button="none"
    </xsl:variable>

    <xsl:output method="html" use-character-maps="map" encoding="utf-8" indent="yes" />
    
    <xsl:character-map name="map">
        <xsl:output-character character="&#8211;" string="&amp;#8211;" />
        <xsl:output-character character="&#8212;" string="&amp;#8212;" />
        <xsl:output-character character="&#8220;" string="&amp;#8220;" />
        <xsl:output-character character="&#8221;" string="&amp;#8221;" />
        <xsl:output-character character="&#8217;" string="&amp;#8217;" />
        <xsl:output-character character="&#8216;" string="&amp;#8216;" />
    </xsl:character-map>

    <xsl:template match="/">
        <xsl:apply-templates select="h:html/h:body" />
    </xsl:template>

    <xsl:template match="h:body">
        <xsl:apply-templates select="(h:h1|h:h2)" />
        
        <xsl:call-template name="generate-toc" />
    </xsl:template>

    <xsl:template match="h:h1|h:h2">
        <xsl:variable name="output" select="concat($outdir, @id, $suffix)" />

        <xsl:variable name="prev" select="(preceding::h:h1|preceding::h:h2)[position()=last()]" />
        <xsl:variable name="next" select="(following::h:h1|following::h:h2)[1]" />

        <xsl:variable name="stop" select="(following-sibling::h:h1|following-sibling::h:h2)[1]" />
        <xsl:variable name="between">
            <xsl:choose>
                <xsl:when test="$stop"><xsl:sequence select="following-sibling::*[. &lt;&lt; $stop]" /></xsl:when>
                <xsl:otherwise><xsl:sequence select="following-sibling::*" /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="crumbs">
            <div class="doc-crumbs">
                <a href="index.html">Foglyn User Guide</a>
                <xsl:text> &gt; </xsl:text>
                <xsl:if test="local-name() = 'h2'">
                    <a>
                        <xsl:attribute name="href" select="concat(preceding-sibling::h:h1[1]/@id, '.html')" />
                        <xsl:value-of select="preceding-sibling::h:h1[1]" />
                    </a>
                    <xsl:text> &gt; </xsl:text>
                </xsl:if>
                <xsl:value-of select="." />
            </div>
        </xsl:variable>

        <xsl:variable name="nav">
            <div class="doc-nav">
                <xsl:if test="$prev">
                    <a class="doc-prev" href="{$prev/@id}.html" accesskey="p">
                        <img src="images/prev.gif" alt="Previous" />
                        <br />
                        <xsl:value-of select="$prev" />
                    </a>
                </xsl:if>
                <xsl:text> </xsl:text>
                <xsl:if test="$next">
                    <a class="doc-next" href="{$next/@id}.html" accesskey="n">
                        <img src="images/next.gif" alt="Next" />
                        <br />
                        <xsl:value-of select="$next" />
                    </a>
                </xsl:if>
            </div>
        </xsl:variable>
            
        <xsl:result-document href="{$output}">
            <xsl:message><xsl:value-of select="$output" /></xsl:message>
            <xsl:text>--- name:metadata&#10;title="</xsl:text>
            <xsl:value-of select="." />
            <xsl:text> - Foglyn User Guide"</xsl:text>
            <xsl:value-of select="$metadata" />
            <xsl:text>--- name:content pipeline:&#10;</xsl:text>
            <xsl:copy-of select="$crumbs" copy-namespaces="no"/>

            <xsl:copy-of select="$nav" copy-namespaces="no" />

            <div class="doc-content">
                <h2><xsl:value-of select="." /></h2>
                <xsl:apply-templates select="$between" mode="copy"/>
            </div>
            
            <xsl:copy-of select="$nav" copy-namespaces="no" />
            <xsl:text>&#10;---</xsl:text>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="h:a[starts-with(@href, 'PLUGINS_ROOT')]" mode="copy">
        <xsl:element name="a">
            <xsl:attribute name="href" select="concat($helpPrefix, substring-after(@href, 'PLUGINS_ROOT'))" />
            <xsl:attribute name="target">_blank</xsl:attribute>
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="@class">
                        <xsl:value-of select="concat(@class, ' external')" />
                    </xsl:when>
                    <xsl:otherwise>external</xsl:otherwise>
                </xsl:choose> 
            </xsl:attribute>
            
            <xsl:copy-of select="@*[local-name() != 'href' and local-name() != 'class']" />
            <xsl:apply-templates mode="copy"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="h:a[starts-with(@href, 'http://') or starts-with(@href, 'https://')]" mode="copy">
        <xsl:element name="a">
            <xsl:attribute name="target">_blank</xsl:attribute>
            <xsl:attribute name="class">
                <xsl:choose>
                    <xsl:when test="@class">
                        <xsl:value-of select="concat(@class, ' external')" />
                    </xsl:when>
                    <xsl:otherwise>external</xsl:otherwise>
                </xsl:choose> 
            </xsl:attribute>
            
            <xsl:copy-of select="@*[local-name() != 'class']" />
            <xsl:apply-templates mode="copy"/>
        </xsl:element>
    </xsl:template>
        
    <xsl:template match="*" mode="copy">
        <xsl:element name="{local-name()}" >
            <xsl:copy-of select="@*" />
            <xsl:apply-templates mode="copy"/>
        </xsl:element>
    </xsl:template>
    
    <xsl:template name="generate-toc">
        <xsl:variable name="output" select="concat($outdir, 'index', $suffix)" />

        <xsl:text>--- name:metadata&#10;title="Table of Contents - Foglyn User Guide"</xsl:text>
        <xsl:value-of select="$metadata" />
        <xsl:text>--- name:content pipeline:&#10;</xsl:text>
        
        <h2>Foglyn User Guide</h2>
        
        <ul>
            <xsl:apply-templates select="h:h1" mode="toc" />
        </ul>
        <xsl:text>&#10;---</xsl:text>
    </xsl:template>
    
    <xsl:template match="h:h1" mode="toc">
        <li>
            <a><xsl:attribute name="href" select="concat(@id, '.html')"/><xsl:value-of select="." /></a>
        
            <xsl:variable name="stop" select="(following-sibling::h:h1)[1]" />
            
            <xsl:choose>
                <xsl:when test="$stop">
                    <xsl:variable name="subs" select="following-sibling::h:h2[. &lt;&lt; $stop]" />
                    <xsl:if test="$subs">
                        <ul><xsl:apply-templates select="$subs" mode="toc" /></ul>
                    </xsl:if>
                </xsl:when>
                
                <xsl:otherwise>
                    <xsl:variable name="subs" select="following-sibling::h:h2" />
                    <xsl:if test="$subs">
                        <ul><xsl:apply-templates select="$subs" mode="toc" /></ul>
                    </xsl:if>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>

    <xsl:template match="h:h2" mode="toc">
        <li><a><xsl:attribute name="href" select="concat(@id, '.html')"/><xsl:value-of select="." /></a></li>
    </xsl:template>
</xsl:stylesheet>
