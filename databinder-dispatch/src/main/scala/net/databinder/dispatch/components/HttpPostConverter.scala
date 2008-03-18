package net.databinder.dispatch.components

import java.util.Locale

import org.apache.wicket.util.convert.IConverter
import org.apache.wicket.util.convert.converters.AbstractConverter
import org.apache.wicket.markup.html.basic.Label
import org.apache.wicket.model.IModel 
import org.apache.wicket.util.io.Streams
import org.apache.wicket._

import org.slf4j.LoggerFactory

import net.sf.ehcache.CacheManager
import net.sf.ehcache.Ehcache
import net.sf.ehcache.Element

import net.databinder.dispatch.Http

abstract class HttpPostConverter extends AbstractConverter {

  def service = new Http("localhost", 8180)
  def path_name: String
  
  def convertToObject(value: String, locale: Locale): Object = null
  
  def getTargetType = classOf[String]

  override def convertToString(source: Object, locale: Locale) = {
    val key = source.hashCode()
    val cache = HttpPostConverter.cache_for(path_name)
    
    cache.get(key) match {
      case null => try {
        val out = (service("/" + path_name) << "input" -> source).as_str
        cache.put(new Element(key, out))
        out
      } catch {
        case e =>
          if (Application.get.getConfigurationType == Application.DEVELOPMENT)
            throw new RestartResponseAtInterceptPageException(new ConnectionErrorPage(e))
          else {
            HttpPostConverter.log.error("Error posting to server", e)
            ""
          }
      }
      case elem => elem.getValue.toString
    }
  }
}

object HttpPostConverter {
	private val log = LoggerFactory.getLogger(classOf[HttpPostConverter])
	
  private def cache_for(path_name: String) = {
    val mgr = CacheManager.getInstance()
    val name = classOf[HttpPostConverter].getName() + ":" + path_name
    val cache = mgr.getEhcache(name)
    if (cache != null)
      cache
    else {
      mgr.addCache(name)
      mgr.getEhcache(name)
    }
  }
}
