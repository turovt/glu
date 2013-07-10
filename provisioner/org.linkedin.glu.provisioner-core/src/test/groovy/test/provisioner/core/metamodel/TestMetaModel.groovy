/*
 * Copyright (c) 2013 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package test.provisioner.core.metamodel

import org.linkedin.util.io.ram.RAMDirectory
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.impl.builder.JsonMetaModelSerializerImpl

/**
 * @author yan@pongasoft.com  */
public class TestMetaModel extends GroovyTestCase
{
  /**
   * Empty model
   */
  public void testEmptyGluMetaModel()
  {
    def model = """{
}"""

    def expectedModel = """
{
  "metaModelVersion": "1.0.0"
}
"""

    checkJson(model, expectedModel)
  }

  /**
   * Test that system wide global values work
   */
  public void testGlobalValues()
  {
    def model = """{
  "gluVersion": "x.y.z",
  "zooKeeperRoot": "/acme",
  "stateMachine": {
    "defaultTransitions": {
      "NONE": [{"to": "s1", "action": "noneTOs1"}],
      "s1": [{"to": "NONE", "action": "s1TOnone"}, {"to": "s2", "action": "s1TOs2"}],
      "s2": [{"to": "s1", "action": "s2TOs1"}]
    },
    "defaultEntryState": "s2"
  }
}"""

    def expectedModel = """
{
  "gluVersion": "x.y.z",
  "metaModelVersion": "1.0.0",
  "stateMachine": {
    "defaultEntryState": "s2",
    "defaultTransitions": {
      "NONE": [
        {
          "action": "noneTOs1",
          "to": "s1"
        }
      ],
      "s1": [
        {
          "action": "s1TOnone",
          "to": "NONE"
        },
        {
          "action": "s1TOs2",
          "to": "s2"
        }
      ],
      "s2": [
        {
          "action": "s2TOs1",
          "to": "s1"
        }
      ]
    }
  },
  "zooKeeperRoot": "/acme"
}
"""

    checkJson(model, expectedModel)
  }

  /**
   * This represents a model similar to the tutorial
   */
  public void testTutorialGluMetaModel()
  {
    def model = new File("../../packaging/org.linkedin.glu.packaging-all/src/cmdline/resources/models/tutorial/glu-meta-model.json.groovy").text

    def expectedModel = """
{
  "agents": [
    {
      "fabric": "glu-dev-1",
      "host": "localhost",
      "name": "agent-1"
    }
  ],
  "consoles": [
    {
      "configTokens": {
        "dataSource": "\\ndataSource.dbCreate ='update'\\ndataSource.url=\\"jdbc:hsqldb:file:\${System.properties['user.dir']}/database/prod;shutdown=true\\"\\n"
      },
      "host": "localhost",
      "name": "tutorialConsole",
      "plugins": [
        {
          "fqcn": "org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin"
        }
      ]
    }
  ],
  "fabrics": {
    "glu-dev-1": {
      "console": "tutorialConsole",
      "keys": {
        "agentKeyStore": {
          "checksum": "JSHZAn5IQfBVp1sy0PgA36fT_fD",
          "keyPassword": "nWVxpMg6Tkv",
          "storePassword": "nacEn92x8-1",
          "uri": "agent.keystore"
        },
        "agentTrustStore": {
          "checksum": "CvFUauURMt-gxbOkkInZ4CIV50y",
          "keyPassword": "nWVxpMg6Tkv",
          "storePassword": "nacEn92x8-1",
          "uri": "agent.truststore"
        },
        "consoleKeyStore": {
          "checksum": "wxiKSyNAHN2sOatUG2qqIpuVYxb",
          "keyPassword": "nWVxpMg6Tkv",
          "storePassword": "nacEn92x8-1",
          "uri": "console.keystore"
        },
        "consoleTrustStore": {
          "checksum": "qUFMIePiJhz8i7Ow9lZmN5pyZjl",
          "storePassword": "nacEn92x8-1",
          "uri": "console.truststore"
        }
      },
      "zooKeeperCluster": "tutorialZooKeeperCluster"
    }
  },
  "gluVersion": "@glu.version@",
  "metaModelVersion": "1.0.0",
  "zooKeeperClusters": [
    {
      "fabrics": [
        "glu-dev-1"
      ],
      "name": "tutorialZooKeeperCluster",
      "zooKeepers": [
        {
          "host": "127.0.0.1",
          "version": "@zookeeper.version@"
        }
      ]
    }
  ]
}"""

    def metaModel = checkJson(model, expectedModel)

    assertEquals(['glu-dev-1'], metaModel.fabrics.keySet() as List)

    // agents
    assertEquals(1, metaModel.agents.size())
    AgentMetaModel agent = metaModel.findAgent('glu-dev-1', 'agent-1')
    assertEquals(metaModel.agents.iterator().next(), agent)
    assertEquals(12906, agent.mainPort)
    assertEquals('localhost', agent.host.resolveHostAddress())

    // consoles
    assertEquals(1, metaModel.consoles.size())
    ConsoleMetaModel console = metaModel.findConsole('tutorialConsole')
    assertEquals(console, metaModel.consoles.values().iterator().next())
    assertEquals(8080, console.mainPort)
    assertEquals('localhost', console.host.resolveHostAddress())

    // zookeeper clusters
    assertEquals(1, metaModel.zooKeeperClusters.size())
    ZooKeeperClusterMetaModel zkCluster = metaModel.findZooKeeperCluster('tutorialZooKeeperCluster')
    assertEquals(zkCluster, metaModel.zooKeeperClusters.values().iterator().next())
    assertEquals('127.0.0.1:2181', zkCluster.zooKeeperConnectionString)
    assertEquals(1, zkCluster.zooKeepers.size())
    ZooKeeperMetaModel zk = zkCluster.zooKeepers[0]
    assertEquals(2181, zk.mainPort)
    assertEquals(2888, zk.quorumPort)
    assertEquals(3888, zk.leaderElectionPort)
    assertEquals('127.0.0.1', zk.host.resolveHostAddress())

  }

  /**
   * Test that whatever is not tested by the tutorial model still works
   */
  public void testMiscModel()
  {
    def model = """{
  "gluVersion": "x.y.z",
  "fabrics": {
    "fabric-1": {
      "console": "console-1",
      "zooKeeperCluster": "zkc-1"
    }
  },
  "agents": [
    {
      "version": "av1",
      "host": "ah1",
      "install": {
        "path": "/a1/"
      },
      "configTokens": {
        "a.p1": "a.v1"
      },
      "name": "agent-1",
      "port": 1111,
      "ports": {
        "configPort": 2222
      },
      "fabric": "fabric-1"
    }
  ],
  "consoles": [
     {
      "version": "cv1",
      "host": "ch1",
      "install": {
        "path": "/c1/"
      },
      "configTokens": {
        "c.p1": "c.v1"
      },
      "name": "console-1",
      "port": 3333,
      "ports": {
        "externalPort": 4444
      },
      "externalHost": "ceh1",
      "internalPath": "/i/c1",
      "externalPath": "/e/c2",
      "plugins": [
        {
          "fqcn": "pl.c1",
          "classPath": ["file:/p.jar"]
        }
      ],
      "dataSourceDriverUri": "file:/d.jar"
    }
  ],

  "zooKeeperClusters": [
    {
      "configTokens": {
        "zkc.p1": "zkc.v1"
      },
      "name": "zkc-1",
      "fabrics": ["fabric-1"],
      "zooKeepers": [
        {
          "version": "z11",
          "host": "z1h1",
          "install": {
            "path": "/z1/"
          },
          "configTokens": {
            "z1.p1": "z1.v1"
          },
          "name": "zk-1",
          "port": 5555,
          "ports": {
            "quorumPort": 6666,
            "leaderElectionPort": 7777
          }
        },
        {
          "version": "z21",
          "host": "z2h1",
          "install": {
            "path": "/z2/"
          },
          "configTokens": {
            "z2.p1": "z2.v1"
          },
          "name": "zk-2",
          "port": 8888,
          "ports": {
            "quorumPort": 9999,
            "leaderElectionPort": 10000
          }
        }
      ]
    }
  ]

}"""

    def expectedModel = """
{
  "agents": [
    {
      "configTokens": {
        "a.p1": "a.v1"
      },
      "fabric": "fabric-1",
      "host": "ah1",
      "install": {
        "path": "/a1/"
      },
      "name": "agent-1",
      "port": 1111,
      "ports": {
        "configPort": 2222
      },
      "version": "av1"
    }
  ],
  "consoles": [
    {
      "configTokens": {
        "c.p1": "c.v1"
      },
      "dataSourceDriverUri": "file:/d.jar",
      "externalHost": "ceh1",
      "externalPath": "/e/c2",
      "host": "ch1",
      "install": {
        "path": "/c1/"
      },
      "internalPath": "/i/c1",
      "name": "console-1",
      "plugins": [
        {
          "classPath": [
            "file:/p.jar"
          ],
          "fqcn": "pl.c1"
        }
      ],
      "port": 3333,
      "ports": {
        "externalPort": 4444
      },
      "version": "cv1"
    }
  ],
  "fabrics": {
    "fabric-1": {
      "console": "console-1",
      "zooKeeperCluster": "zkc-1"
    }
  },
  "gluVersion": "x.y.z",
  "metaModelVersion": "1.0.0",
  "zooKeeperClusters": [
    {
      "configTokens": {
        "zkc.p1": "zkc.v1"
      },
      "fabrics": [
        "fabric-1"
      ],
      "name": "zkc-1",
      "zooKeepers": [
        {
          "configTokens": {
            "z1.p1": "z1.v1"
          },
          "host": "z1h1",
          "install": {
            "path": "/z1/"
          },
          "port": 5555,
          "ports": {
            "leaderElectionPort": 7777,
            "quorumPort": 6666
          },
          "version": "z11"
        },
        {
          "configTokens": {
            "z2.p1": "z2.v1"
          },
          "host": "z2h1",
          "install": {
            "path": "/z2/"
          },
          "port": 8888,
          "ports": {
            "leaderElectionPort": 10000,
            "quorumPort": 9999
          },
          "version": "z21"
        }
      ]
    }
  ]
}
"""

    checkJson(model, expectedModel)
  }

  private GluMetaModel checkJson(String model, String expectedModel)
  {
    RAMDirectory ram = new RAMDirectory()
    ram.add('model', model)
    JsonMetaModelSerializerImpl serializer = new JsonMetaModelSerializerImpl()
    def metaModel = serializer.deserialize([ram.toResource().createRelative('/model')])
    assertEquals((expectedModel ?: model).trim(), serializer.serialize(metaModel, true))
    return metaModel
  }
}