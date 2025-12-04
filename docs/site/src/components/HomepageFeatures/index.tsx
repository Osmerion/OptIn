import type {ReactNode} from 'react';
import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  Svg: React.ComponentType<React.ComponentProps<'svg'>>;
  description: ReactNode;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Consentful API Access',
    Svg: require('@site/static/img/undraw_docusaurus_mountain.svg').default,
    description: (
      <>
        OptIn defines annotations that can be used by libraries and applications
        to require and to declare explicit opt-in into APIs.
      </>
    ),
  },
  {
    title: 'Compile-Time Safety',
    Svg: require('@site/static/img/undraw_docusaurus_tree.svg').default,
    description: (
      <>
        OptIn provides tooling that enforces correct use of opt-in requirements
        at compile-time to ensure that unstable APIs are used intentionally.
      </>
    ),
  },
  {
    title: 'Third-Party Compatibility',
    Svg: require('@site/static/img/undraw_docusaurus_react.svg').default,
    description: (
      <>
        OptIn allows customizing it first-party tooling to integrate seamlessly
        with third-party libraries without opt-in support (e.g., Guava's <code>@Beta</code>).
      </>
    ),
  },
];

function Feature({title, Svg, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): ReactNode {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
