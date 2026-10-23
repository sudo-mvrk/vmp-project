# Volunteer Recommendation Scoring Function

Formal specification of the task-to-volunteer scoring model, based on Multi-Attribute Utility Theory (MAUT).

## Notation

| Symbol | Meaning |
|---|---|
| $V$ | Set of all volunteers |
| $v \in V$ | A volunteer |
| $t$ | A task |
| $d(v,t)$ | Distance between volunteer $v$ and task $t$ |
| $S_v, S_t \in \mathbb{R}^n_{\ge 0}$ | Skill vectors of volunteer $v$ and task $t$ |
| $P(t)$ | Raw scalar priority of task $t$, on the scale $[P_{\min}, P_{\max}]$ |
| $\lambda > 0$ | Distance-decay parameter |
| $(\alpha,\beta,\gamma)$ | Weights set by the analytical department |

## 1. Utility Components

Each attribute is mapped onto a common, dimensionless scale $[0,1]$, so the components are commensurable.

### 1.1 Geospatial utility

$$
u_{geo}(v,t) = f_{geo}\big(d(v,t)\big) = \exp\!\big(-\lambda\, d(v,t)\big) \in [0,1]
$$

Alternative with a hard operational radius $d_{max}$:

$$
u_{geo}(v,t) = \max\!\left(0,\; 1 - \frac{d(v,t)}{d_{max}}\right)
$$

### 1.2 Skill-relevance utility

$$
u_{skill}(v,t) = f_{skill}(S_v, S_t) = \frac{\tilde S_v \cdot S_t}{\lVert \tilde S_v \rVert \, \lVert S_t \rVert} \in [0,1],
\qquad
\tilde S_{v,i} = \begin{cases} S_{v,i} & \text{if } S_{t,i} > 0 \\ 0 & \text{otherwise} \end{cases}
$$

(cosine similarity between the task vector and the volunteer vector projected onto the skills the task requires; for non-negative skill vectors the value is always in $[0,1]$). Skills the task does not require do not lower the score; only missing required skills do.

Edge cases: $S_t = 0$ (no skills required) gives $u_{skill} = 1$; $\tilde S_v = 0$ (none of the required skills) gives $u_{skill} = 0$.

### 1.3 Priority utility

$$
u_{prio}(t) = \frac{P(t) - P_{\min}}{P_{\max} - P_{\min}} \in [0,1]
$$

$P_{\min}$ and $P_{\max}$ are the bounds of the priority scale, not of the tasks currently in the pool. With the scale LOW = 1, MEDIUM = 2, HIGH = 3, URGENT = 4: $P_{\min} = 1$, $P_{\max} = 4$, so $u_{prio} \in \{0, \tfrac13, \tfrac23, 1\}$.

Fixed bounds make $u_{prio}(t)$ a property of the task alone: the score does not change when other tasks are added or closed, which keeps results reproducible.

## 2. Weight Constraint

$$
(\alpha,\beta,\gamma) \in \Delta^2 = \left\{ (\alpha,\beta,\gamma) \in \mathbb{R}^3_{\ge 0} : \alpha + \beta + \gamma = 1 \right\}
$$

## 3. Aggregate Score

$$
R(v,t) = \alpha\, u_{geo}(v,t) + \beta\, u_{skill}(v,t) + \gamma\, u_{prio}(t)
$$

Because every $u_i \in [0,1]$ and the weights lie on the simplex, $R(v,t) \in [0,1]$.

## 4. Optimal Assignment

$$
v^{*} = \operatorname*{arg\,max}_{v \,\in\, \mathcal{V}_{feas}(t)} R(v,t)
$$

**Feasible set:**

$$
\mathcal{V}_{feas}(t) = \left\{ v \in V : \text{Available}(v,t)=1,\ \text{Certified}(v,t)=1,\ \text{Cap}(v) > 0 \right\}
$$

**Tie-breaking** (lexicographic): prefer larger $u_{skill}$, then larger $u_{geo}$.

## 5. Optional Aggregation Variants

The additive model above is fully compensatory. If proximity (or another attribute) should act as a gate, use one of these instead.

**Multiplicative (Cobb–Douglas, partially non-compensatory):**

$$
R(v,t) = u_{geo}(v,t)^{\alpha} \cdot u_{skill}(v,t)^{\beta} \cdot u_{prio}(t)^{\gamma}
$$

**Weighted power mean (tunable compensability via $p$):**

$$
R_p(v,t) = \Big( \alpha\, u_{geo}(v,t)^p + \beta\, u_{skill}(v,t)^p + \gamma\, u_{prio}(t)^p \Big)^{1/p}
$$

- $p = 1$: additive model (Section 3)
- $p \to 0$: multiplicative model
- $p \to -\infty$: $\min\{u_{geo}, u_{skill}, u_{prio}\}$ (fully non-compensatory)

## 6. Properties Guaranteed by This Formulation

- $R(v,t) \in [0,1]$: bounded and comparable across tasks
- Weights form a proper convex combination (MAUT-consistent)
- Each attribute is dimensionless and independently interpretable
- The aggregation rule (additive / compensatory) is explicit, not implicit
- Assignment is a well-defined constrained argmax over an explicit feasible set

## 7. Compact Summary

$$
\begin{aligned}
R(v,t) &= \alpha\, u_{geo}(v,t) + \beta\, u_{skill}(v,t) + \gamma\, u_{prio}(t), \\
u_{geo}(v,t) &= \exp(-\lambda\, d(v,t)) \in [0,1], \\
u_{skill}(v,t) &= \frac{\tilde S_v \cdot S_t}{\lVert \tilde S_v \rVert \, \lVert S_t \rVert} \in [0,1], \\
u_{prio}(t) &= \frac{P(t) - P_{\min}}{P_{\max} - P_{\min}} \in [0,1], \\
(\alpha,\beta,\gamma) &\in \Delta^2, \qquad v^{*} = \operatorname*{arg\,max}_{v \in \mathcal{V}_{feas}(t)} R(v,t).
\end{aligned}
$$
