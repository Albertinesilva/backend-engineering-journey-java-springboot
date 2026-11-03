package com.albertsilva.dev.asjcatalog.validation.user.dns;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Component;

/**
 * Implementação de {@link MxRecordLookup} que consulta o DNS real pela API
 * JNDI, com o provedor {@code com.sun.jndi.dns.DnsContextFactory}.
 *
 * <p>
 * Não há servidor, timeout nem tentativas configurados no código: valem os
 * padrões do provedor e o DNS do sistema operacional. Só o tipo MX é
 * considerado; não há alternativa para domínios que aceitam e-mail apenas por
 * registro A/AAAA. <b>Qualquer {@link Exception}</b> (domínio inexistente, DNS
 * indisponível, tempo esgotado etc.) resulta em {@code false}, o que faz uma
 * falha de rede tornar inválido um e-mail que talvez fosse válido.
 * </p>
 *
 * <p>
 * Cada chamada cria um contexto JNDI novo e o fecha ao final. A consulta é
 * <b>síncrona</b>, na thread de quem chama.
 * </p>
 */
@Component
public class JndiMxRecordLookup implements MxRecordLookup {

  /**
   * Cria o contexto JNDI usado em cada consulta. Existe para que os testes de
   * unidade possam fornecer um contexto falso, sem acessar a rede.
   */
  @FunctionalInterface
  interface DirContextFactory {

    /**
     * @return novo contexto JNDI
     * @throws NamingException se o contexto não puder ser criado
     */
    DirContext create() throws NamingException;
  }

  private final DirContextFactory contextFactory;

  /**
   * Construtor usado pelo Spring: cada consulta cria um
   * {@link InitialDirContext} com o provedor de DNS do JNDI.
   */
  public JndiMxRecordLookup() {
    this(JndiMxRecordLookup::createDnsContext);
  }

  /**
   * Construtor usado pelos testes de unidade, que fornecem a fábrica do
   * contexto.
   *
   * @param contextFactory fábrica do contexto JNDI
   */
  JndiMxRecordLookup(DirContextFactory contextFactory) {
    this.contextFactory = contextFactory;
  }

  /**
   * Consulta o DNS e verifica se o domínio possui ao menos um registro do tipo
   * MX. O contexto JNDI é fechado no bloco {@code finally}.
   *
   * @param domain domínio a consultar
   * @return {@code true} se houver ao menos um registro MX; {@code false} se não
   *         houver ou se ocorrer qualquer erro
   */
  @Override
  public boolean hasMxRecord(String domain) {

    DirContext context = null;

    try {

      context = contextFactory.create();

      Attributes attributes = context.getAttributes(domain, new String[] { "MX" });

      Attribute attribute = attributes.get("MX");

      return attribute != null && attribute.size() > 0;

    } catch (Exception e) {

      return false;

    } finally {
      closeContext(context);
    }
  }

  /**
   * Cria o contexto JNDI de DNS.
   *
   * @return novo contexto
   * @throws NamingException se o contexto não puder ser criado
   */
  private static DirContext createDnsContext() throws NamingException {

    Hashtable<String, String> env = new Hashtable<>();

    env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

    return new InitialDirContext(env);
  }

  /**
   * Fecha o contexto JNDI de forma segura, capturando
   * qualquer exceção que possa ocorrer durante o encerramento.
   *
   * @param context contexto JNDI a ser fechado
   */
  private void closeContext(DirContext context) {

    if (context == null) {
      return;
    }

    try {
      context.close();
    } catch (Exception e) {
      // ignora erro ao fechar
    }
  }
}
